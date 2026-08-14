package com.linkforge.controller

import com.linkforge.dto.UrlAnalyticsResponse
import com.linkforge.repository.ClickEventRepository
import com.linkforge.repository.UrlRepository
import com.linkforge.exception.UrlNotFoundException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/v1/urls")
@Tag(name = "URL Analytics", description = "Endpoints for URL analytics and click tracking")
class UrlAnalyticsController(
    private val urlRepository: UrlRepository,
    private val clickEventRepository: ClickEventRepository
) {

    @GetMapping("/{shortCode}/analytics")
    @Operation(summary = "Get analytics for a shortened URL", description = "Returns click statistics for the given short code and optional domain")
    fun getAnalytics(
        @PathVariable shortCode: String,
        @RequestParam(required = false) domain: String?
    ): ResponseEntity<UrlAnalyticsResponse> {
        
        val url = if (domain.isNullOrBlank()) {
            urlRepository.findByShortCodeAndDomainIsNull(shortCode)
        } else {
            urlRepository.findByShortCodeAndDomain_Domain(shortCode, domain)
        } ?: throw UrlNotFoundException("Short URL not found for code: $shortCode and domain: $domain")

        val totalClicks = clickEventRepository.countByUrlId(url.id)
        val firstClick = clickEventRepository.findFirstByUrlIdOrderByClickedAtAsc(url.id)?.clickedAt
        val lastClick = clickEventRepository.findFirstByUrlIdOrderByClickedAtDesc(url.id)?.clickedAt
        
        val clicksByDayRaw = clickEventRepository.getClicksByDay(url.id)
        val clicksByDay = clicksByDayRaw.associate { row ->
            val dateStr = row[0].toString()
            val count = (row[1] as Number).toLong()
            dateStr to count
        }

        val response = UrlAnalyticsResponse(
            totalClicks = totalClicks,
            firstClick = firstClick,
            lastClick = lastClick,
            clicksByDay = clicksByDay
        )

        return ResponseEntity.ok(response)
    }
}
