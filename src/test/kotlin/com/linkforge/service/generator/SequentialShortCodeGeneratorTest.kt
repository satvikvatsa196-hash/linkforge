package com.linkforge.service.generator

import com.linkforge.model.Url
import com.linkforge.repository.UrlRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*

class SequentialShortCodeGeneratorTest {

    private lateinit var urlRepository: UrlRepository
    private lateinit var generator: SequentialShortCodeGenerator

    @BeforeEach
    fun setUp() {
        urlRepository = mock(UrlRepository::class.java)
        generator = SequentialShortCodeGenerator(urlRepository)
    }

    @Test
    fun `generate should create temp url, get ID, encode, and save again`() {
        val originalUrl = "https://example.com"
        
        `when`(urlRepository.save(any(Url::class.java) ?: Url(originalUrl=""))).thenAnswer { invocation ->
            val url = invocation.getArgument<Url>(0)
            if (url.id == 0L) {
                // Mock returning a saved entity with ID 1000
                Url(id = 1000L, originalUrl = url.originalUrl, shortCode = url.shortCode)
            } else {
                url // Return updated entity
            }
        }

        val shortCode = generator.generate(originalUrl)

        // 1000 encoded in Base62 is "G8"
        assertEquals("G8", shortCode)
        verify(urlRepository, times(2)).save(any(Url::class.java) ?: Url(originalUrl=""))
    }
}
