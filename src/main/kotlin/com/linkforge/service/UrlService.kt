package com.linkforge.service

import com.linkforge.dto.UrlShortenRequest
import com.linkforge.dto.UrlShortenResponse
import com.linkforge.exception.InvalidUrlException
import com.linkforge.exception.UrlNotFoundException
import com.linkforge.model.Url
import com.linkforge.repository.UrlRepository
import com.linkforge.service.generator.ShortCodeGenerator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI

@Service
class UrlService(
    private val urlRepository: UrlRepository,
    private val shortCodeGenerator: ShortCodeGenerator,
    @Value("\${app.base-url:http://localhost:8080}") private val baseUrl: String
) {
    
    private val log = LoggerFactory.getLogger(UrlService::class.java)

    @Transactional
    fun shortenUrl(request: UrlShortenRequest): UrlShortenResponse {
        val originalUrl = request.originalUrl
        
        // Validate URL just in case, though DTO has @URL
        if (!isValidUrl(originalUrl)) {
            throw InvalidUrlException("Invalid URL format: $originalUrl")
        }

        // Check for existing URL
        urlRepository.findByOriginalUrl(originalUrl)?.let { existingUrl ->
            log.info("Found existing short URL for: {}", originalUrl)
            return toResponse(existingUrl)
        }

        // Generate actual short code using the selected strategy
        val actualShortCode = shortCodeGenerator.generate(originalUrl)

        // Get the updated/saved url from DB to retrieve its ID or created_at
        val updatedUrl = urlRepository.findByShortCode(actualShortCode) 
            ?: throw IllegalStateException("Could not find newly created URL with short code: $actualShortCode")

        log.info("Created new short URL: {} -> {}", originalUrl, actualShortCode)
        return toResponse(updatedUrl)
    }

    @Transactional
    fun getOriginalUrl(shortCode: String): String {
        val url = urlRepository.findByShortCode(shortCode)
            ?: throw UrlNotFoundException("Short URL not found for code: $shortCode")

        // Increment click count
        url.clicksCount += 1
        urlRepository.save(url)
        
        log.info("Redirecting short code {} to {}", shortCode, url.originalUrl)
        return url.originalUrl
    }

    private fun toResponse(url: Url): UrlShortenResponse {
        val shortUrl = "$baseUrl/${url.shortCode}"
        return UrlShortenResponse(
            shortCode = url.shortCode,
            originalUrl = url.originalUrl,
            shortUrl = shortUrl,
            createdAt = url.createdAt
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
