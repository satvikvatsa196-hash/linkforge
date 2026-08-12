package com.linkforge.controller

import com.linkforge.exception.UrlNotFoundException
import com.linkforge.service.UrlService
import com.linkforge.service.UrlRedirectInfo
import com.linkforge.service.ClickTrackingService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(RedirectController::class)
class RedirectControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var urlService: UrlService

    @MockBean
    private lateinit var clickTrackingService: ClickTrackingService

    @Test
    fun `redirect should return 302 Found and location header`() {
        val shortCode = "1"
        val originalUrl = "https://example.com"
        val redirectInfo = UrlRedirectInfo(1L, originalUrl)
        
        `when`(urlService.getOriginalUrl(shortCode)).thenReturn(redirectInfo)

        mockMvc.perform(get("/$shortCode"))
            .andExpect(status().isFound)
            .andExpect(header().string("Location", originalUrl))
    }

    @Test
    fun `redirect should return 404 if short code not found`() {
        val shortCode = "invalid"
        
        `when`(urlService.getOriginalUrl(shortCode)).thenThrow(UrlNotFoundException::class.java)

        mockMvc.perform(get("/$shortCode"))
            .andExpect(status().isNotFound)
    }
}
