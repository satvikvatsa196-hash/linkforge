package com.linkforge.service.generator

import com.linkforge.model.Url
import com.linkforge.repository.UrlRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*

class RandomShortCodeGeneratorTest {

    private lateinit var urlRepository: UrlRepository
    private lateinit var generator: RandomShortCodeGenerator

    @BeforeEach
    fun setUp() {
        urlRepository = mock(UrlRepository::class.java)
        generator = RandomShortCodeGenerator(urlRepository)
    }

    @Test
    fun `generate should create a random 7 character short code and save it`() {
        val originalUrl = "https://example.com"
        `when`(urlRepository.findByShortCode(anyString())).thenReturn(null) // No collisions

        val shortCode = generator.generate(originalUrl)

        assertNotNull(shortCode)
        assertEquals(7, shortCode.length)
        
        // Mockito any() issues bypassed by just checking invocations
        verify(urlRepository, times(1)).findByShortCode(anyString())
        verify(urlRepository, times(1)).save(org.mockito.ArgumentMatchers.any(Url::class.java) ?: Url(originalUrl=""))
    }

    @Test
    fun `generate should retry on collision and eventually succeed`() {
        val originalUrl = "https://example.com"
        
        // Return an existing URL for the first 2 attempts, then null (success)
        `when`(urlRepository.findByShortCode(anyString()))
            .thenReturn(Url(id=1, originalUrl="https://other.com", shortCode="coll1"))
            .thenReturn(Url(id=2, originalUrl="https://other.com", shortCode="coll2"))
            .thenReturn(null)

        val shortCode = generator.generate(originalUrl)

        assertNotNull(shortCode)
        assertEquals(7, shortCode.length)
        
        verify(urlRepository, times(3)).findByShortCode(anyString())
        verify(urlRepository, times(1)).save(org.mockito.ArgumentMatchers.any(Url::class.java) ?: Url(originalUrl=""))
    }

    @Test
    fun `generate should throw RuntimeException if max collisions reached`() {
        val originalUrl = "https://example.com"
        
        // Always return collision
        `when`(urlRepository.findByShortCode(anyString()))
            .thenReturn(Url(id=1, originalUrl="https://other.com", shortCode="coll"))

        val exception = assertThrows<RuntimeException> {
            generator.generate(originalUrl)
        }

        assertTrue(exception.message!!.contains("Could not generate a unique short code"))
        verify(urlRepository, times(10)).findByShortCode(anyString())
        verify(urlRepository, never()).save(org.mockito.ArgumentMatchers.any(Url::class.java) ?: Url(originalUrl=""))
    }
}
