package com.linkforge

import com.linkforge.dto.UrlShortenRequest
import com.linkforge.dto.UrlShortenResponse
import com.linkforge.model.Url
import com.linkforge.repository.UrlRepository
import com.linkforge.service.UrlCleanupService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpStatus
import java.time.OffsetDateTime
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UrlExpirationIntegrationTest(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val urlRepository: UrlRepository,
    @Autowired val redisTemplate: StringRedisTemplate,
    @Autowired val urlCleanupService: UrlCleanupService
) {

    @BeforeEach
    fun setup() {
        urlRepository.deleteAll()
        redisTemplate.connectionFactory?.connection?.serverCommands()?.flushDb()
    }

    @Test
    fun `creation without expiration`() {
        val request = UrlShortenRequest("https://google.com", null)
        val response = restTemplate.postForEntity("/api/v1/urls", request, UrlShortenResponse::class.java)
        
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.expiresAt).isNull()
    }

    @Test
    fun `valid expiration`() {
        val future = OffsetDateTime.now().plusDays(1)
        val request = UrlShortenRequest("https://example.com", future)
        val response = restTemplate.postForEntity("/api/v1/urls", request, UrlShortenResponse::class.java)
        
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.expiresAt).isNotNull()
    }

    @Test
    fun `invalid past expiration returns 400`() {
        val past = OffsetDateTime.now().minusDays(1)
        val request = UrlShortenRequest("https://example.com", past)
        val response = restTemplate.postForEntity("/api/v1/urls", request, Any::class.java)
        
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `redirect before expiration`() {
        val future = OffsetDateTime.now().plusDays(1)
        val request = UrlShortenRequest("https://example.com/redirect", future)
        val createResponse = restTemplate.postForEntity("/api/v1/urls", request, UrlShortenResponse::class.java)
        val shortCode = createResponse.body!!.shortCode

        val redirectResponse = restTemplate.getForEntity("/$shortCode", Any::class.java)
        assertThat(redirectResponse.statusCode).isEqualTo(HttpStatus.FOUND)
    }

    @Test
    fun `redirect after expiration returns 410`() {
        val shortCode = "expired1"
        val url = Url(
            originalUrl = "https://example.com/expired",
            shortCode = shortCode,
            expiresAt = OffsetDateTime.now().minusDays(1)
        )
        urlRepository.save(url)

        val redirectResponse = restTemplate.getForEntity("/$shortCode", Any::class.java)
        assertThat(redirectResponse.statusCode).isEqualTo(HttpStatus.GONE)
    }

    @Test
    fun `Redis cached expired URL is not served`() {
        val originalUrl = "https://example.com/cached"
        val request = UrlShortenRequest(originalUrl, OffsetDateTime.now().plusSeconds(2))
        val response = restTemplate.postForEntity("/api/v1/urls", request, UrlShortenResponse::class.java)
        val code = response.body!!.shortCode
        
        Thread.sleep(3000) // Wait for TTL to expire
        
        val redirectResponse = restTemplate.getForEntity("/$code", Any::class.java)
        assertThat(redirectResponse.statusCode).isEqualTo(HttpStatus.GONE)
    }

    @Test
    fun `cleanup task marks expired as inactive and invalidates cache`() {
        val shortCode = "cleanup1"
        val originalUrl = "https://example.com/cleanup"
        val url = Url(
            originalUrl = originalUrl,
            shortCode = shortCode,
            expiresAt = OffsetDateTime.now().minusDays(1)
        )
        urlRepository.save(url)

        redisTemplate.opsForValue().set("url:$shortCode", "${url.id}|$originalUrl")
        assertThat(redisTemplate.hasKey("url:$shortCode")).isTrue()

        urlCleanupService.cleanupExpiredUrls()

        val updated = urlRepository.findByShortCode(shortCode)!!
        assertThat(updated.inactive).isTrue()
        assertThat(redisTemplate.hasKey("url:$shortCode")).isFalse()
    }
}
