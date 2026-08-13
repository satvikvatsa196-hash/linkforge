package com.linkforge

import com.linkforge.dto.UrlAnalyticsResponse
import com.linkforge.dto.UrlShortenRequest
import com.linkforge.dto.UrlShortenResponse
import com.linkforge.model.Url
import com.linkforge.repository.ClickEventRepository
import com.linkforge.repository.UrlRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.time.OffsetDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ClickTrackingIntegrationTest(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val urlRepository: UrlRepository,
    @Autowired val clickEventRepository: ClickEventRepository,
    @Autowired val redisTemplate: StringRedisTemplate
) {

    @org.springframework.boot.test.mock.mockito.MockBean
    lateinit var rabbitTemplate: org.springframework.amqp.rabbit.core.RabbitTemplate

    @BeforeEach
    fun setup() {
        clickEventRepository.deleteAll()
        urlRepository.deleteAll()
        redisTemplate.connectionFactory?.connection?.serverCommands()?.flushDb()
    }

    @Test
    fun `successful click is recorded with headers`() {
        // Create URL
        val request = UrlShortenRequest("https://google.com")
        val createResponse = restTemplate.postForEntity("/api/v1/urls", request, UrlShortenResponse::class.java)
        val shortCode = createResponse.body!!.shortCode

        // Perform click with headers
        val headers = HttpHeaders()
        headers.set("User-Agent", "TestAgent")
        headers.set("Referer", "https://referer.com")
        headers.set("X-Forwarded-For", "192.168.1.1")
        
        val redirectResponse = restTemplate.exchange(
            "/$shortCode", 
            HttpMethod.GET, 
            HttpEntity<String>(headers), 
            Any::class.java
        )
        
        assertThat(redirectResponse.statusCode).isEqualTo(HttpStatus.FOUND)

        // Verify click event is published
        org.mockito.Mockito.verify(rabbitTemplate).convertAndSend(
            org.mockito.ArgumentMatchers.eq(com.linkforge.config.RabbitMQConfig.EXCHANGE_NAME),
            org.mockito.ArgumentMatchers.eq(com.linkforge.config.RabbitMQConfig.ROUTING_KEY),
            org.mockito.ArgumentMatchers.any(com.linkforge.dto.ClickEventMessage::class.java)
        )
    }

    @Test
    fun `failed redirect is not recorded`() {
        val redirectResponse = restTemplate.getForEntity("/invalid-code", Any::class.java)
        assertThat(redirectResponse.statusCode).isEqualTo(HttpStatus.NOT_FOUND)

        org.mockito.Mockito.verifyNoInteractions(rabbitTemplate)
    }

    @Test
    fun `expired URL is not recorded`() {
        val shortCode = "expired-click"
        val url = Url(
            originalUrl = "https://example.com",
            shortCode = shortCode,
            expiresAt = OffsetDateTime.now().minusDays(1)
        )
        urlRepository.save(url)

        val redirectResponse = restTemplate.getForEntity("/$shortCode", Any::class.java)
        assertThat(redirectResponse.statusCode).isEqualTo(HttpStatus.GONE)

        org.mockito.Mockito.verifyNoInteractions(rabbitTemplate)
    }

    @Test
    fun `analytics API returns correct calculations`() {
        val request = UrlShortenRequest("https://analytics.com")
        val createResponse = restTemplate.postForEntity("/api/v1/urls", request, UrlShortenResponse::class.java)
        val shortCode = createResponse.body!!.shortCode

        // Manually save clicks since we are mocking RabbitMQ
        val url = urlRepository.findByShortCode(shortCode)!!
        clickEventRepository.save(com.linkforge.model.ClickEvent(urlId = url.id, ipHash = "hash1", clickedAt = OffsetDateTime.now().minusMinutes(5)))
        clickEventRepository.save(com.linkforge.model.ClickEvent(urlId = url.id, ipHash = "hash2", clickedAt = OffsetDateTime.now()))

        // Get analytics
        val analyticsResponse = restTemplate.getForEntity("/api/v1/urls/$shortCode/analytics", UrlAnalyticsResponse::class.java)
        assertThat(analyticsResponse.statusCode).isEqualTo(HttpStatus.OK)
        
        val analytics = analyticsResponse.body!!
        assertThat(analytics.totalClicks).isEqualTo(2)
        assertThat(analytics.firstClick).isNotNull()
        assertThat(analytics.lastClick).isNotNull()
        assertThat(analytics.lastClick).isAfter(analytics.firstClick)
        assertThat(analytics.clicksByDay).isNotEmpty()
    }
}
