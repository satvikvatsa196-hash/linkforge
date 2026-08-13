package com.linkforge.service

import com.linkforge.dto.UrlShortenRequest
import com.linkforge.dto.UrlShortenResponse
import com.linkforge.exception.AliasAlreadyExistsException
import com.linkforge.exception.InvalidAliasException
import com.linkforge.exception.InvalidUrlException
import com.linkforge.exception.UrlExpiredException
import com.linkforge.exception.UrlNotFoundException
import com.linkforge.model.Url
import com.linkforge.repository.UrlRepository
import com.linkforge.service.generator.ShortCodeGenerator
import org.slf4j.LoggerFactory

data class UrlRedirectInfo(
    val id: Long,
    val originalUrl: String
)

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.redis.core.StringRedisTemplate
import java.net.URI
import java.time.Duration
import java.time.OffsetDateTime
import java.util.concurrent.CompletableFuture

@Service
class UrlService(
    private val urlRepository: UrlRepository,
    private val shortCodeGenerator: ShortCodeGenerator,
    private val redisTemplate: StringRedisTemplate,
    @Value("\${app.base-url:http://localhost:8080}") private val baseUrl: String,
    @Value("\${app.cache.ttl:24h}") private val cacheTtl: Duration,
    @Value("\${app.reserved-aliases:api,health,actuator,swagger,docs}") private val reservedAliases: List<String>
) {
    
    private val log = LoggerFactory.getLogger(UrlService::class.java)

    private fun getCacheKey(shortCode: String) = "url:$shortCode"

    private fun getFromCache(shortCode: String): UrlRedirectInfo? {
        return try {
            val cached = redisTemplate.opsForValue().get(getCacheKey(shortCode)) ?: return null
            val parts = cached.split("|", limit = 2)
            if (parts.size == 2) {
                UrlRedirectInfo(parts[0].toLong(), parts[1])
            } else {
                null // Legacy format, force miss
            }
        } catch (e: Exception) {
            log.warn("Redis cache read failed for {}: {}", shortCode, e.message)
            null
        }
    }

    private fun putInCache(shortCode: String, originalUrl: String, urlId: Long, expiresAt: OffsetDateTime? = null) {
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
                redisTemplate.opsForValue().set(getCacheKey(shortCode), "$urlId|$originalUrl", ttl)
            } catch (e: Exception) {
                log.warn("Redis cache write failed for {}: {}", shortCode, e.message)
            }
        }
    }

    private fun deleteFromCache(shortCode: String) {
        try {
            redisTemplate.delete(getCacheKey(shortCode))
        } catch (e: Exception) {
            log.warn("Redis cache delete failed for {}: {}", shortCode, e.message)
        }
    }

    @Transactional
    fun shortenUrl(request: UrlShortenRequest): UrlShortenResponse {
        val originalUrl = request.originalUrl
        val expiresAt = request.expiresAt
        val alias = request.alias
        
        // Validate URL just in case, though DTO has @URL
        if (!isValidUrl(originalUrl)) {
            throw InvalidUrlException("Invalid URL format: $originalUrl")
        }

        if (alias != null) {
            if (reservedAliases.contains(alias.lowercase())) {
                throw InvalidAliasException("Alias is reserved")
            }
            if (urlRepository.findByShortCode(alias) != null) {
                throw AliasAlreadyExistsException("Alias already exists")
            }
            
            // Create a new URL entry with the provided alias
            val url = Url(
                originalUrl = originalUrl,
                shortCode = alias,
                expiresAt = expiresAt
            )
            val updatedUrl = urlRepository.save(url)
            log.info("Created new short URL with alias: {} -> {}", originalUrl, alias)
            putInCache(alias, originalUrl, updatedUrl.id, expiresAt)
            return toResponse(updatedUrl)
        }

        // Check for existing URL (only if no custom alias was provided)
        urlRepository.findByOriginalUrl(originalUrl)?.let { existingUrl ->
            log.info("Found existing short URL for: {}", originalUrl)
            if (existingUrl.expiresAt != expiresAt || existingUrl.inactive) {
                existingUrl.expiresAt = expiresAt
                existingUrl.inactive = false
                urlRepository.save(existingUrl)
            }
            // Populate cache
            putInCache(existingUrl.shortCode, existingUrl.originalUrl, existingUrl.id, existingUrl.expiresAt)
            return toResponse(existingUrl)
        }

        // Generate actual short code using the selected strategy
        val updatedUrl = shortCodeGenerator.generate(originalUrl, expiresAt)

        log.info("Created new short URL: {} -> {}", originalUrl, updatedUrl.shortCode)
        
        // Populate cache automatically
        putInCache(updatedUrl.shortCode, originalUrl, updatedUrl.id, expiresAt)
        
        return toResponse(updatedUrl)
    }

    @Transactional(readOnly = true)
    fun getOriginalUrl(shortCode: String): UrlRedirectInfo {
        // Check cache first
        val cachedUrl = getFromCache(shortCode)
        if (cachedUrl != null) {
            log.info("Cache hit for short code: {}", shortCode)
            return cachedUrl
        }
        
        log.info("Cache miss for short code: {}", shortCode)
        
        val url = urlRepository.findByShortCode(shortCode)
            ?: throw UrlNotFoundException("Short URL not found for code: $shortCode")

        if (url.inactive) {
            throw UrlNotFoundException("Short URL is inactive")
        }

        if (url.expiresAt != null && url.expiresAt!!.isBefore(OffsetDateTime.now())) {
            throw UrlExpiredException("Short URL has expired")
        }

        // Populate cache on miss
        putInCache(shortCode, url.originalUrl, url.id, url.expiresAt)
        
        log.info("Redirecting short code {} to {}", shortCode, url.originalUrl)
        return UrlRedirectInfo(url.id, url.originalUrl)
    }

    fun invalidateCache(shortCode: String) {
        deleteFromCache(shortCode)
        log.info("Invalidated cache for short code: {}", shortCode)
    }

    @Transactional(readOnly = true)
    fun getUrlForQr(shortCode: String): String {
        val url = urlRepository.findByShortCode(shortCode)
            ?: throw UrlNotFoundException("Short URL not found for code: $shortCode")

        if (url.inactive) {
            throw UrlNotFoundException("Short URL is inactive")
        }

        if (url.expiresAt != null && url.expiresAt!!.isBefore(OffsetDateTime.now())) {
            throw UrlExpiredException("Short URL has expired")
        }

        return "$baseUrl/$shortCode"
    }

    private fun toResponse(url: Url): UrlShortenResponse {
        val shortUrl = "$baseUrl/${url.shortCode}"
        return UrlShortenResponse(
            shortCode = url.shortCode,
            originalUrl = url.originalUrl,
            shortUrl = shortUrl,
            createdAt = url.createdAt,
            expiresAt = url.expiresAt
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
}
