package com.linkforge

import com.linkforge.dto.*
import com.linkforge.model.ClickEvent
import com.linkforge.model.Url
import com.linkforge.repository.ClickEventRepository
import com.linkforge.repository.UrlRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.time.OffsetDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DashboardAnalyticsIntegrationTest(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val urlRepository: UrlRepository,
    @Autowired val clickEventRepository: ClickEventRepository
) {

    @BeforeEach
    fun setup() {
        clickEventRepository.deleteAll()
        urlRepository.deleteAll()
    }

    @Test
    fun `empty analytics`() {
        val now = OffsetDateTime.now()
        val from = now.minusDays(1).toString()
        val to = now.plusDays(1).toString()

        val response = restTemplate.getForEntity(
            "/api/v1/analytics/overview?from=$from&to=$to",
            DashboardOverviewResponse::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = response.body!!
        assertThat(body.totalClicks).isEqualTo(0)
        assertThat(body.uniqueVisitors).isEqualTo(0)
        assertThat(body.activeUrls).isEqualTo(0)
        assertThat(body.totalUrls).isEqualTo(0)
    }

    @Test
    fun `invalid date ranges`() {
        val now = OffsetDateTime.now()
        val from = now.plusDays(1).toString()
        val to = now.minusDays(1).toString()

        val response = restTemplate.getForEntity(
            "/api/v1/analytics/overview?from=$from&to=$to",
            Any::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `aggregation correctness and date filters`() {
        // Setup data
        val url = urlRepository.save(Url(originalUrl = "https://example.com", shortCode = "ex1"))
        
        val now = OffsetDateTime.now()
        // Outside range
        clickEventRepository.save(ClickEvent(urlId = url.id, ipHash = "ip1", clickedAt = now.minusDays(10)))
        
        // Inside range
        clickEventRepository.save(ClickEvent(urlId = url.id, ipHash = "ip1", clickedAt = now.minusHours(1)))
        clickEventRepository.save(ClickEvent(urlId = url.id, ipHash = "ip2", clickedAt = now.minusHours(2)))

        val from = now.minusDays(1).toString()
        val to = now.plusDays(1).toString()

        val response = restTemplate.getForEntity(
            "/api/v1/analytics/overview?from=$from&to=$to",
            DashboardOverviewResponse::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = response.body!!
        assertThat(body.totalClicks).isEqualTo(2) // Only 2 in range
        assertThat(body.uniqueVisitors).isEqualTo(2)
        assertThat(body.activeUrls).isEqualTo(1)
        assertThat(body.totalUrls).isEqualTo(1)
    }

    @Test
    fun `pagination for performance`() {
        val u1 = urlRepository.save(Url(originalUrl = "https://a.com", shortCode = "a"))
        val u2 = urlRepository.save(Url(originalUrl = "https://b.com", shortCode = "b"))
        val u3 = urlRepository.save(Url(originalUrl = "https://c.com", shortCode = "c"))

        val now = OffsetDateTime.now()

        // u1 has 3 clicks
        repeat(3) { clickEventRepository.save(ClickEvent(urlId = u1.id, ipHash = "ip1", clickedAt = now)) }
        // u2 has 5 clicks
        repeat(5) { clickEventRepository.save(ClickEvent(urlId = u2.id, ipHash = "ip1", clickedAt = now)) }
        // u3 has 1 click
        clickEventRepository.save(ClickEvent(urlId = u3.id, ipHash = "ip1", clickedAt = now))

        val from = now.minusDays(1).toString()
        val to = now.plusDays(1).toString()

        val responseType = object : ParameterizedTypeReference<RestResponsePage<UrlPerformanceDto>>() {}
        
        // Page 0, size 2
        val response1 = restTemplate.exchange(
            "/api/v1/analytics/performance?from=$from&to=$to&page=0&size=2",
            HttpMethod.GET,
            null,
            responseType
        )

        assertThat(response1.statusCode).isEqualTo(HttpStatus.OK)
        val page1 = response1.body!!
        assertThat(page1.content).hasSize(2)
        assertThat(page1.content[0].shortCode).isEqualTo("b") // 5 clicks
        assertThat(page1.content[1].shortCode).isEqualTo("a") // 3 clicks

        // Page 1, size 2
        val response2 = restTemplate.exchange(
            "/api/v1/analytics/performance?from=$from&to=$to&page=1&size=2",
            HttpMethod.GET,
            null,
            responseType
        )

        assertThat(response2.statusCode).isEqualTo(HttpStatus.OK)
        val page2 = response2.body!!
        assertThat(page2.content).hasSize(1)
        assertThat(page2.content[0].shortCode).isEqualTo("c") // 1 click
    }

    @Test
    fun `detailed url analytics`() {
        val url = urlRepository.save(Url(originalUrl = "https://test.com", shortCode = "detail"))
        val now = OffsetDateTime.now()

        clickEventRepository.save(ClickEvent(urlId = url.id, ipHash = "ip1", clickedAt = now, userAgent = "Mozilla/5.0 Chrome/90.0", referrer = "Google"))
        clickEventRepository.save(ClickEvent(urlId = url.id, ipHash = "ip2", clickedAt = now, userAgent = "Mozilla/5.0 Mobile Safari/90.0", referrer = "Google"))

        val from = now.minusDays(1).toString()
        val to = now.plusDays(1).toString()

        val response = restTemplate.getForEntity(
            "/api/v1/analytics/urls/detail?from=$from&to=$to",
            UrlDetailedAnalyticsResponse::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = response.body!!
        assertThat(body.totalClicks).isEqualTo(2)
        assertThat(body.referrers["Google"]).isEqualTo(2)
        assertThat(body.browsers["Chrome"]).isEqualTo(1)
        assertThat(body.devices["Mobile"]).isEqualTo(1)
    }

    // Helper to deserialize Page response
    class RestResponsePage<T> : PageImpl<T> {
        constructor() : super(emptyList())
        constructor(content: List<T>) : super(content)
        // Needed for Jackson deserialization
        var size: Int = 0
        var totalElements: Long = 0
        var totalPages: Int = 0
        var number: Int = 0
    }
}
