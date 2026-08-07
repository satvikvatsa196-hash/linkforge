package com.linkforge.controller

import com.linkforge.exception.UrlNotFoundException
import com.linkforge.service.UrlService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
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

    @Test
    fun `redirect should return 302 Found and location header`() {
        val shortCode = "1"
        val originalUrl = "https://example.com"
        
        `when`(urlService.getOriginalUrl(shortCode)).thenReturn(originalUrl)

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
