package com.linkforge.service

import com.linkforge.dto.UrlShortenRequest
import com.linkforge.dto.UrlShortenResponse
import com.linkforge.exception.AliasAlreadyExistsException
import com.linkforge.exception.InvalidAliasException
import com.linkforge.exception.InvalidUrlException
import com.linkforge.exception.UrlExpiredException
import com.linkforge.exception.UrlNotFoundException
import com.linkforge.model.Url
import com.linkforge.model.Domain
import com.linkforge.repository.UrlRepository
import com.linkforge.repository.DomainRepository
import com.linkforge.service.generator.ShortCodeGenerator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.redis.core.StringRedisTemplate
import java.net.URI
import java.time.Duration
import java.time.OffsetDateTime
import java.util.concurrent.CompletableFuture

data class UrlRedirectInfo(
    val id: Long,
    val originalUrl: String
)

@Service
class UrlService(
    private val urlRepository: UrlRepository,
    private val domainRepository: DomainRepository,
    private val shortCodeGenerator: ShortCodeGenerator,
    private val redisTemplate: StringRedisTemplate,
    @Value("\${app.base-url:http://localhost:8080}") private val baseUrl: String,
    @Value("\${app.cache.ttl:24h}") private val cacheTtl: Duration,
    @Value("\${app.reserved-aliases:api,health,actuator,swagger,docs}") private val reservedAliases: List<String>,
    private val metricsTracker: com.linkforge.util.MetricsTracker
) {
    
    private val log = LoggerFactory.getLogger(UrlService::class.java)

    private val defaultHost = URI.create(baseUrl).host

    private fun getCacheKey(shortCode: String, domain: String?): String {
        val actualDomain = if (domain == defaultHost) null else domain
        return if (actualDomain.isNullOrEmpty()) "url:$shortCode" else "url:$actualDomain:$shortCode"
    }

    private fun getFromCache(shortCode: String, domain: String?): UrlRedirectInfo? {
        return try {
            val cached = redisTemplate.opsForValue().get(getCacheKey(shortCode, domain)) ?: return null
            val parts = cached.split("|", limit = 2)
            if (parts.size == 2) {
                UrlRedirectInfo(parts[0].toLong(), parts[1])
            } else {
                null
            }
        } catch (e: Exception) {
            log.warn("Redis cache read failed for {}: {}", shortCode, e.message)
            null
        }
    }

    private fun putInCache(shortCode: String, domain: String?, originalUrl: String, urlId: Long, expiresAt: OffsetDateTime? = null) {
        CompletableFuture.runAsync {
            try {
                var ttl = cacheTtl
                if (expiresAt != null) {
                    val timeToExpiration = Duration.between(OffsetDateTime.now(), expiresAt)
                    if (timeToExpiration < ttl) {
                        ttl = timeToExpiration
                    }
                    if (ttl.isNegative || ttl.isZero) {
                        return@runAsync
                    }
                }
                redisTemplate.opsForValue().set(getCacheKey(shortCode, domain), "$urlId|$originalUrl", ttl)
            } catch (e: Exception) {
                log.warn("Redis cache write failed for {}: {}", shortCode, e.message)
            }
        }
    }

    private fun deleteFromCache(shortCode: String, domain: String?) {
        try {
            redisTemplate.delete(getCacheKey(shortCode, domain))
        } catch (e: Exception) {
            log.warn("Redis cache delete failed for {}: {}", shortCode, e.message)
        }
    }

    @Transactional
    fun shortenUrl(request: UrlShortenRequest): UrlShortenResponse {
        val originalUrl = request.originalUrl
        val expiresAt = request.expiresAt
        val alias = request.alias
        
        if (!isValidUrl(originalUrl)) {
            throw InvalidUrlException("Invalid URL format: $originalUrl")
        }

        var customDomain: Domain? = null
        if (!request.domain.isNullOrBlank() && request.domain != defaultHost) {
            val domainStr = request.domain.lowercase()
            customDomain = domainRepository.findByDomain(domainStr)
            if (customDomain == null) {
                customDomain = domainRepository.save(Domain(domain = domainStr))
            } else if (!customDomain.active) {
                throw InvalidUrlException("Custom domain is inactive")
            }
        }

        if (alias != null) {
            if (reservedAliases.contains(alias.lowercase()) && customDomain == null) {
                throw InvalidAliasException("Alias is reserved")
            }
            
            val existing = if (customDomain == null) {
                urlRepository.findByShortCodeAndDomainIsNull(alias)
            } else {
                urlRepository.findByShortCodeAndDomain_Domain(alias, customDomain.domain)
            }
            
            if (existing != null) {
                throw AliasAlreadyExistsException("Alias already exists on this domain")
            }
            
            val url = Url(
                originalUrl = originalUrl,
                shortCode = alias,
                expiresAt = expiresAt,
                domain = customDomain
            )
            val updatedUrl = urlRepository.save(url)
            log.info("Created new short URL with alias: {} -> {} on domain {}", originalUrl, alias, customDomain?.domain)
            putInCache(alias, customDomain?.domain, originalUrl, updatedUrl.id, expiresAt)
            metricsTracker.recordUrlCreated()
            return toResponse(updatedUrl)
        }

        // Check for existing URL (only if no custom alias was provided)
        val existingUrl = if (customDomain == null) {
            urlRepository.findByOriginalUrlAndDomainIsNull(originalUrl)
        } else {
            urlRepository.findByOriginalUrlAndDomain_Domain(originalUrl, customDomain.domain)
        }
        
        existingUrl?.let { 
            log.info("Found existing short URL for: {}", originalUrl)
            if (it.expiresAt != expiresAt || it.inactive) {
                it.expiresAt = expiresAt
                it.inactive = false
                urlRepository.save(it)
            }
            putInCache(it.shortCode, it.domain?.domain, it.originalUrl, it.id, it.expiresAt)
            return toResponse(it)
        }

        val updatedUrlBase = shortCodeGenerator.generate(originalUrl, expiresAt)
        // Generator creates a Url, but we need to assign the domain
        updatedUrlBase.domain = customDomain
        val updatedUrl = urlRepository.save(updatedUrlBase)

        log.info("Created new short URL: {} -> {}", originalUrl, updatedUrl.shortCode)
        putInCache(updatedUrl.shortCode, customDomain?.domain, originalUrl, updatedUrl.id, expiresAt)
        metricsTracker.recordUrlCreated()
        
        return toResponse(updatedUrl)
    }

    fun getOriginalUrl(shortCode: String, domain: String? = null): UrlRedirectInfo {
        val actualDomain = if (domain == defaultHost) null else domain
        
        val cachedUrl = getFromCache(shortCode, actualDomain)
        if (cachedUrl != null) {
            log.debug("Cache hit for short code: {} on domain {}", shortCode, actualDomain)
            metricsTracker.recordCacheHit()
            return cachedUrl
        }
        metricsTracker.recordCacheMiss()
        
        val url = findValidUrlEntity(shortCode, actualDomain)

        putInCache(shortCode, actualDomain, url.originalUrl, url.id, url.expiresAt)
        return UrlRedirectInfo(url.id, url.originalUrl)
    }

    fun invalidateCache(shortCode: String, domain: String? = null) {
        val actualDomain = if (domain == defaultHost) null else domain
        deleteFromCache(shortCode, actualDomain)
    }

    @Transactional(readOnly = true)
    fun getUrlForQr(shortCode: String, domain: String? = null): String {
        val actualDomain = if (domain == defaultHost) null else domain
        
        val url = findValidUrlEntity(shortCode, actualDomain)

        val prefix = if (actualDomain.isNullOrBlank()) baseUrl else "http://$actualDomain"
        return "$prefix/$shortCode"
    }

    private fun toResponse(url: Url): UrlShortenResponse {
        val prefix = if (url.domain == null) baseUrl else "http://${url.domain!!.domain}"
        val shortUrl = "$prefix/${url.shortCode}"
        return UrlShortenResponse(
            shortCode = url.shortCode,
            originalUrl = url.originalUrl,
            shortUrl = shortUrl,
            createdAt = url.createdAt,
            expiresAt = url.expiresAt,
            domain = url.domain?.domain
        )
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = URI(url)
            uri.scheme == "http" || uri.scheme == "https"
        } catch (e: Exception) {
            false
        }
    }

    private fun findValidUrlEntity(shortCode: String, domain: String?): Url {
        val url = if (domain.isNullOrBlank()) {
            urlRepository.findByShortCodeAndDomainIsNull(shortCode)
        } else {
            urlRepository.findByShortCodeAndDomain_Domain(shortCode, domain)
        } ?: throw UrlNotFoundException("Short URL not found")

        if (url.inactive || (url.domain != null && !url.domain!!.active)) {
            throw UrlNotFoundException("Short URL or domain is inactive")
        }

        if (url.expiresAt != null && url.expiresAt!!.isBefore(OffsetDateTime.now())) {
            throw UrlExpiredException("Short URL has expired")
        }
        
        return url
    }
}
