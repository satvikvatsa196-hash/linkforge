package com.linkforge.service

import com.linkforge.dto.UrlShortenRequest
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

private fun <T> safeEq(value: T): T {
    org.mockito.ArgumentMatchers.eq(value)
    return value
}


class UrlServiceTest {

    private lateinit var urlRepository: UrlRepository
    private lateinit var shortCodeGenerator: ShortCodeGenerator
    private lateinit var urlService: UrlService

    @BeforeEach
    fun setUp() {
        urlRepository = mock(UrlRepository::class.java)
        shortCodeGenerator = mock(ShortCodeGenerator::class.java)
        urlService = UrlService(urlRepository, shortCodeGenerator, "http://test.com")
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
        `when`(shortCodeGenerator.generate(originalUrl)).thenReturn(expectedShortCode)
        `when`(urlRepository.findByShortCode(expectedShortCode)).thenReturn(expectedSavedUrl)

        val response = urlService.shortenUrl(UrlShortenRequest(originalUrl))

        assertEquals(expectedShortCode, response.shortCode)
        assertEquals("http://test.com/random", response.shortUrl)
        assertEquals(originalUrl, response.originalUrl)
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
    fun `getOriginalUrl should return URL and increment clicks if exists`() {
        val existingUrl = Url(id = 1, originalUrl = "https://example.com", shortCode = "1")
        existingUrl.clicksCount = 5
        `when`(urlRepository.findByShortCode("1")).thenReturn(existingUrl)

        val result = urlService.getOriginalUrl("1")

        assertEquals("https://example.com", result)
        assertEquals(6, existingUrl.clicksCount)
        verify(urlRepository).save(existingUrl)
    }

    @Test
    fun `getOriginalUrl should throw UrlNotFoundException if code does not exist`() {
        `when`(urlRepository.findByShortCode("invalid")).thenReturn(null)

        assertThrows<UrlNotFoundException> {
            urlService.getOriginalUrl("invalid")
        }
        
        verify(urlRepository, never()).save(any())
    }
}
