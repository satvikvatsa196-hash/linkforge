package com.linkforge

import com.linkforge.dto.UrlShortenRequest
import com.linkforge.dto.UrlShortenResponse
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
    "app.rate-limit.anonymous.requests=2",
    "app.rate-limit.anonymous.window-ms=2000"
])
@ActiveProfiles("test")
class RateLimitIntegrationTest(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val redisTemplate: StringRedisTemplate
) {

    @BeforeEach
    fun setup() {
        redisTemplate.connectionFactory?.connection?.serverCommands()?.flushDb()
    }

    @Test
    fun `under limit allows requests`() {
        val request = UrlShortenRequest("https://google.com", null)
        val response = restTemplate.postForEntity("/api/v1/urls", request, UrlShortenResponse::class.java)
        
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `exactly at limit allows requests`() {
        val request = UrlShortenRequest("https://google.com", null)
        restTemplate.postForEntity("/api/v1/urls", request, UrlShortenResponse::class.java)
        val response = restTemplate.postForEntity("/api/v1/urls", request, UrlShortenResponse::class.java)
        
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `over limit returns 429`() {
        val request = UrlShortenRequest("https://google.com", null)
        
        // 2 allowed requests
        restTemplate.postForEntity("/api/v1/urls", request, UrlShortenResponse::class.java)
        restTemplate.postForEntity("/api/v1/urls", request, UrlShortenResponse::class.java)
        
        // 3rd request should be blocked
        val response = restTemplate.postForEntity("/api/v1/urls", request, String::class.java)
        
        assertThat(response.statusCode).isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
        assertThat(response.headers.getFirst("Retry-After")).isNotNull()
    }

    @Test
    fun `separate clients get separate limits`() {
        val request = UrlShortenRequest("https://google.com", null)

        // Client 1 (IP 192.168.1.1)
        val headers1 = HttpHeaders()
        headers1.add("X-Forwarded-For", "192.168.1.1")
        val entity1 = HttpEntity(request, headers1)

        restTemplate.exchange("/api/v1/urls", HttpMethod.POST, entity1, UrlShortenResponse::class.java)
        restTemplate.exchange("/api/v1/urls", HttpMethod.POST, entity1, UrlShortenResponse::class.java)
        
        val response1 = restTemplate.exchange("/api/v1/urls", HttpMethod.POST, entity1, String::class.java)
        assertThat(response1.statusCode).isEqualTo(HttpStatus.TOO_MANY_REQUESTS)

        // Client 2 (IP 192.168.1.2) - should still be allowed
        val headers2 = HttpHeaders()
        headers2.add("X-Forwarded-For", "192.168.1.2")
        val entity2 = HttpEntity(request, headers2)

        val response2 = restTemplate.exchange("/api/v1/urls", HttpMethod.POST, entity2, UrlShortenResponse::class.java)
        assertThat(response2.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `window reset allows requests again`() {
        val request = UrlShortenRequest("https://google.com", null)
        
        restTemplate.postForEntity("/api/v1/urls", request, UrlShortenResponse::class.java)
        restTemplate.postForEntity("/api/v1/urls", request, UrlShortenResponse::class.java)
        
        val blockedResponse = restTemplate.postForEntity("/api/v1/urls", request, String::class.java)
        assertThat(blockedResponse.statusCode).isEqualTo(HttpStatus.TOO_MANY_REQUESTS)

        // Wait for window to expire (2000ms configured in properties)
        Thread.sleep(2100)

        val successResponse = restTemplate.postForEntity("/api/v1/urls", request, UrlShortenResponse::class.java)
        assertThat(successResponse.statusCode).isEqualTo(HttpStatus.OK)
    }
}
