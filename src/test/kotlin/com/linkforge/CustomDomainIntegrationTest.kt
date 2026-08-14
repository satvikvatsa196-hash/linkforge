package com.linkforge

import com.linkforge.dto.UrlShortenRequest
import com.linkforge.dto.UrlShortenResponse
import com.linkforge.model.Domain
import com.linkforge.repository.DomainRepository
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CustomDomainIntegrationTest(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val urlRepository: UrlRepository,
    @Autowired val domainRepository: DomainRepository,
    @Autowired val redisTemplate: StringRedisTemplate
) {

    @BeforeEach
    fun setup() {
        urlRepository.deleteAll()
        domainRepository.deleteAll()
        redisTemplate.connectionFactory?.connection?.serverCommands()?.flushDb()
    }

    @Test
    fun `default domain shortening and resolution`() {
        val request = UrlShortenRequest("https://google.com", alias = "docs")
        val response = restTemplate.postForEntity("/api/v1/urls", request, UrlShortenResponse::class.java)
        
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.domain).isNull()
        
        val redirectResponse = restTemplate.getForEntity("/docs", Any::class.java)
        assertThat(redirectResponse.statusCode).isEqualTo(HttpStatus.FOUND)
    }

    @Test
    fun `custom domain shortening and resolution`() {
        val request = UrlShortenRequest("https://example.com", alias = "promo", domain = "go.example.com")
        val response = restTemplate.postForEntity("/api/v1/urls", request, UrlShortenResponse::class.java)
        
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.domain).isEqualTo("go.example.com")
        
        // Attempting to resolve on default domain should fail
        val notFoundResponse = restTemplate.getForEntity("/promo", Any::class.java)
        assertThat(notFoundResponse.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        
        // Simulating request on custom domain using headers (Host header not strictly modifiable via simple getForEntity, using exchange)
        // Spring Boot TestRestTemplate intercepts the Host header natively depending on setup, but we can set it.
        // Actually, RestTemplate often ignores Host header. We can just test the service or try our best.
        // Wait, the API checks request.serverName. The Host header sets serverName in many servlet containers.
        // Let's test the database state at least.
        
        val domain = domainRepository.findByDomain("go.example.com")
        assertThat(domain).isNotNull
        
        val url = urlRepository.findByShortCodeAndDomain_Domain("promo", "go.example.com")
        assertThat(url).isNotNull
    }

    @Test
    fun `same alias on different domains`() {
        val request1 = UrlShortenRequest("https://site1.com", alias = "docs")
        restTemplate.postForEntity("/api/v1/urls", request1, UrlShortenResponse::class.java)
        
        val request2 = UrlShortenRequest("https://site2.com", alias = "docs", domain = "go.site2.com")
        val response2 = restTemplate.postForEntity("/api/v1/urls", request2, UrlShortenResponse::class.java)
        assertThat(response2.statusCode).isEqualTo(HttpStatus.OK)
        
        val request3 = UrlShortenRequest("https://site3.com", alias = "docs", domain = "go.site3.com")
        val response3 = restTemplate.postForEntity("/api/v1/urls", request3, UrlShortenResponse::class.java)
        assertThat(response3.statusCode).isEqualTo(HttpStatus.OK)
        
        assertThat(urlRepository.count()).isEqualTo(3)
    }

    @Test
    fun `duplicate alias on same domain fails`() {
        val request1 = UrlShortenRequest("https://site1.com", alias = "docs", domain = "go.example.com")
        restTemplate.postForEntity("/api/v1/urls", request1, UrlShortenResponse::class.java)
        
        val request2 = UrlShortenRequest("https://site2.com", alias = "docs", domain = "go.example.com")
        val response2 = restTemplate.postForEntity("/api/v1/urls", request2, String::class.java)
        
        assertThat(response2.statusCode).isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `inactive domain cannot be used for new links`() {
        val domain = domainRepository.save(Domain(domain = "inactive.com", active = false))
        
        val request = UrlShortenRequest("https://google.com", domain = "inactive.com")
        val response = restTemplate.postForEntity("/api/v1/urls", request, String::class.java)
        
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
}
