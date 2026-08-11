package com.linkforge.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.linkforge.dto.UrlShortenRequest
import com.linkforge.dto.UrlShortenResponse
import com.linkforge.service.UrlService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.ArgumentMatchers.anyString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import com.linkforge.service.QrCodeService
import com.linkforge.exception.UrlNotFoundException
import com.linkforge.exception.UrlExpiredException
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

    @MockBean
    private lateinit var qrCodeService: QrCodeService

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

    @Test
    fun `shortenUrl should return 200 for valid custom alias`() {
        val request = UrlShortenRequest("https://example.com", alias = "my-valid-alias1")
        val response = UrlShortenResponse(
            shortCode = "my-valid-alias1",
            originalUrl = "https://example.com",
            shortUrl = "http://localhost:8080/my-valid-alias1",
            createdAt = OffsetDateTime.now()
        )

        `when`(urlService.shortenUrl(request)).thenReturn(response)

        mockMvc.perform(
            post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.shortCode").value("my-valid-alias1"))
    }

    @Test
    fun `shortenUrl should return 400 for alias with spaces`() {
        val request = UrlShortenRequest("https://example.com", alias = "my alias")

        mockMvc.perform(
            post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `shortenUrl should return 400 for alias with special characters`() {
        val request = UrlShortenRequest("https://example.com", alias = "my@alias!")

        mockMvc.perform(
            post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `shortenUrl should return 400 for too short alias`() {
        val request = UrlShortenRequest("https://example.com", alias = "ab")

        mockMvc.perform(
            post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `shortenUrl should return 400 for too long alias`() {
        val request = UrlShortenRequest("https://example.com", alias = "a".repeat(51))

        mockMvc.perform(
            post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getQrCode should return 200 and image png`() {
        val shortCode = "myalias"
        val fullUrl = "http://localhost:8080/myalias"
        val mockQrBytes = byteArrayOf(1, 2, 3, 4, 5)

        `when`(urlService.getUrlForQr(shortCode)).thenReturn(fullUrl)
        `when`(qrCodeService.generateQrCode(fullUrl)).thenReturn(mockQrBytes)

        mockMvc.perform(get("/api/v1/urls/$shortCode/qr"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE))
            .andExpect(content().bytes(mockQrBytes))
    }

    @Test
    fun `getQrCode should return 404 for invalid shortCode`() {
        val shortCode = "nonexistent"

        `when`(urlService.getUrlForQr(shortCode)).thenThrow(UrlNotFoundException("URL not found"))

        mockMvc.perform(get("/api/v1/urls/$shortCode/qr"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `getQrCode should return 410 for expired URL`() {
        val shortCode = "expired"

        `when`(urlService.getUrlForQr(shortCode)).thenThrow(UrlExpiredException("URL has expired"))

        mockMvc.perform(get("/api/v1/urls/$shortCode/qr"))
            .andExpect(status().isGone)
    }
}
