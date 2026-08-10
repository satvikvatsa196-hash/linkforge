package com.linkforge.service

import com.linkforge.dto.UrlShortenRequest
import com.linkforge.exception.AliasAlreadyExistsException
import com.linkforge.exception.InvalidAliasException
import com.linkforge.exception.InvalidUrlException
import com.linkforge.exception.UrlNotFoundException
import com.linkforge.model.Url
import com.linkforge.repository.UrlRepository
import com.linkforge.service.generator.ShortCodeGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

private fun <T> safeEq(value: T): T {
    org.mockito.ArgumentMatchers.eq(value)
    return value
}


class UrlServiceTest {

    private lateinit var urlRepository: UrlRepository
    private lateinit var shortCodeGenerator: ShortCodeGenerator
    private lateinit var redisTemplate: StringRedisTemplate
    private lateinit var valueOps: ValueOperations<String, String>
    private lateinit var urlService: UrlService

    @BeforeEach
    @Suppress("UNCHECKED_CAST")
    fun setUp() {
        urlRepository = mock(UrlRepository::class.java)
        shortCodeGenerator = mock(ShortCodeGenerator::class.java)
        redisTemplate = mock(StringRedisTemplate::class.java)
        valueOps = mock(ValueOperations::class.java) as ValueOperations<String, String>
        
        `when`(redisTemplate.opsForValue()).thenReturn(valueOps)
        
        urlService = UrlService(urlRepository, shortCodeGenerator, redisTemplate, "http://test.com", Duration.ofHours(24), listOf("api", "health", "actuator", "swagger", "docs"))
    }

    @Test
    fun `shortenUrl should return existing URL if it already exists`() {
        val originalUrl = "https://example.com"
        val existingUrl = Url(id = 1, originalUrl = originalUrl, shortCode = "1")
        `when`(urlRepository.findByOriginalUrl(originalUrl)).thenReturn(existingUrl)

        val response = urlService.shortenUrl(UrlShortenRequest(originalUrl))

        assertEquals("1", response.shortCode)
        assertEquals("http://test.com/1", response.shortUrl)
        assertEquals(originalUrl, response.originalUrl)
        verify(urlRepository, never()).save(any())
    }

    @Test
    fun `shortenUrl should generate new short code and save if URL does not exist`() {
        val originalUrl = "https://example.com"
        val expectedShortCode = "random"
        val expectedSavedUrl = Url(id = 1L, originalUrl = originalUrl, shortCode = expectedShortCode)
        
        `when`(urlRepository.findByOriginalUrl(originalUrl)).thenReturn(null)
        `when`(shortCodeGenerator.generate(originalUrl)).thenReturn(expectedSavedUrl)

        val response = urlService.shortenUrl(UrlShortenRequest(originalUrl))

        assertEquals(expectedShortCode, response.shortCode)
        assertEquals("http://test.com/random", response.shortUrl)
        assertEquals(originalUrl, response.originalUrl)
        
        // Wait briefly for CompletableFuture to execute in test (since putInCache is async)
        Thread.sleep(100)
        verify(valueOps).set(safeEq("url:random"), safeEq(originalUrl), any(Duration::class.java) ?: Duration.ofHours(24))
    }

    @Test
    fun `shortenUrl should throw InvalidUrlException for invalid URL`() {
        val invalidUrl = "not-a-valid-url"

        assertThrows<InvalidUrlException> {
            urlService.shortenUrl(UrlShortenRequest(invalidUrl))
        }
        
        verify(urlRepository, never()).findByOriginalUrl(anyString())
    }

    @Test
    fun `getOriginalUrl should return URL from DB on cache miss and save to cache`() {
        val existingUrl = Url(id = 1, originalUrl = "https://example.com", shortCode = "1")
        existingUrl.clicksCount = 5
        
        `when`(valueOps.get("url:1")).thenReturn(null)
        `when`(urlRepository.findByShortCode("1")).thenReturn(existingUrl)

        val result = urlService.getOriginalUrl("1")

        assertEquals("https://example.com", result)
        assertEquals(6, existingUrl.clicksCount)
        verify(urlRepository).save(existingUrl)
        
        Thread.sleep(100)
        verify(valueOps).set(safeEq("url:1"), safeEq("https://example.com"), any(Duration::class.java) ?: Duration.ofHours(24))
    }

    @Test
    fun `getOriginalUrl should return URL from cache on hit and update DB minimally`() {
        `when`(valueOps.get("url:1")).thenReturn("https://example.com")

        val result = urlService.getOriginalUrl("1")

        assertEquals("https://example.com", result)
        verify(urlRepository).incrementClickCount("1")
        verify(urlRepository, never()).findByShortCode(anyString())
    }

    @Test
    fun `getOriginalUrl should fall back to DB when Redis throws exception`() {
        val existingUrl = Url(id = 1, originalUrl = "https://example.com", shortCode = "1")
        
        `when`(valueOps.get("url:1")).thenThrow(RuntimeException("Redis is down"))
        `when`(urlRepository.findByShortCode("1")).thenReturn(existingUrl)

        val result = urlService.getOriginalUrl("1")

        // Should successfully return the URL despite the exception
        assertEquals("https://example.com", result)
        verify(urlRepository).findByShortCode("1")
    }

    @Test
    fun `getOriginalUrl should throw UrlNotFoundException if code does not exist`() {
        `when`(urlRepository.findByShortCode("invalid")).thenReturn(null)

        assertThrows<UrlNotFoundException> {
            urlService.getOriginalUrl("invalid")
        }
        
        verify(urlRepository, never()).save(any())
    }

    @Test
    fun `shortenUrl should save and return custom alias`() {
        val originalUrl = "https://example.com"
        val customAlias = "my-custom-link"
        val expectedSavedUrl = Url(id = 1L, originalUrl = originalUrl, shortCode = customAlias)

        `when`(urlRepository.findByShortCode(customAlias)).thenReturn(null)
        `when`(urlRepository.save(any(Url::class.java))).thenReturn(expectedSavedUrl)

        val response = urlService.shortenUrl(UrlShortenRequest(originalUrl, alias = customAlias))

        assertEquals(customAlias, response.shortCode)
        assertEquals("http://test.com/my-custom-link", response.shortUrl)
        assertEquals(originalUrl, response.originalUrl)

        verify(urlRepository).save(any(Url::class.java))
    }

    @Test
    fun `shortenUrl should throw AliasAlreadyExistsException if alias exists`() {
        val originalUrl = "https://example.com"
        val customAlias = "my-custom-link"

        `when`(urlRepository.findByShortCode(customAlias)).thenReturn(Url(id = 1L, originalUrl = "https://other.com", shortCode = customAlias))

        assertThrows<AliasAlreadyExistsException> {
            urlService.shortenUrl(UrlShortenRequest(originalUrl, alias = customAlias))
        }
        
        verify(urlRepository, never()).save(any())
    }

    @Test
    fun `shortenUrl should throw InvalidAliasException for reserved alias`() {
        val originalUrl = "https://example.com"
        
        assertThrows<InvalidAliasException> {
            urlService.shortenUrl(UrlShortenRequest(originalUrl, alias = "api"))
        }

        assertThrows<InvalidAliasException> {
            urlService.shortenUrl(UrlShortenRequest(originalUrl, alias = "HEALTH"))
        }
        
        verify(urlRepository, never()).save(any())
    }
}
