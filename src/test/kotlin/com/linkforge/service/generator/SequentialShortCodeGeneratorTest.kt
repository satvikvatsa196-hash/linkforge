package com.linkforge.service.generator

import com.linkforge.model.Url
import com.linkforge.repository.UrlRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.*
import java.util.Optional

class SequentialShortCodeGeneratorTest {

    private lateinit var urlRepository: UrlRepository
    private lateinit var generator: SequentialShortCodeGenerator

    @BeforeEach
    fun setUp() {
        urlRepository = mock(UrlRepository::class.java)
        generator = SequentialShortCodeGenerator(urlRepository)
    }

    @Test
    fun `generate should fetch sequence, encode, insert natively, and return Url`() {
        val originalUrl = "https://example.com"
        val expectedId = 1000L
        val expectedShortCode = "G8"
        val expectedUrl = Url(id = expectedId, originalUrl = originalUrl, shortCode = expectedShortCode)
        
        `when`(urlRepository.getNextSequenceValue()).thenReturn(expectedId)
        `when`(urlRepository.findById(expectedId)).thenReturn(Optional.of(expectedUrl))

        val generatedUrl = generator.generate(originalUrl)

        // 1000 encoded in Base62 is "G8"
        assertEquals(expectedShortCode, generatedUrl.shortCode)
        
        verify(urlRepository).getNextSequenceValue()
        verify(urlRepository).insertUrlWithId(eq(expectedId), eq(originalUrl), eq(expectedShortCode))
        verify(urlRepository).findById(expectedId)
    }
}
