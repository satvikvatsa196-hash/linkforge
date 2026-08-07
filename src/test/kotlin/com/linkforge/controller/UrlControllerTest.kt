package com.linkforge.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.linkforge.dto.UrlShortenRequest
import com.linkforge.dto.UrlShortenResponse
import com.linkforge.service.UrlService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.OffsetDateTime

@WebMvcTest(UrlController::class)
class UrlControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var urlService: UrlService

    @Test
    fun `shortenUrl should return 200 and short code`() {
        val request = UrlShortenRequest("https://example.com")
        val response = UrlShortenResponse(
            shortCode = "1",
            originalUrl = "https://example.com",
            shortUrl = "http://localhost:8080/1",
            createdAt = OffsetDateTime.now()
        )

        `when`(urlService.shortenUrl(request)).thenReturn(response)

        mockMvc.perform(
            post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.shortCode").value("1"))
            .andExpect(jsonPath("$.originalUrl").value("https://example.com"))
            .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/1"))
    }

    @Test
    fun `shortenUrl should return 400 for invalid URL format`() {
        val request = UrlShortenRequest("invalid-url")

        mockMvc.perform(
            post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }
}
