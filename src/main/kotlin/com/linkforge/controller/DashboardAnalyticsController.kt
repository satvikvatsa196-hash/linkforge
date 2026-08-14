package com.linkforge.controller

import com.linkforge.dto.*
import com.linkforge.repository.ClickEventRepository
import com.linkforge.repository.UrlRepository
import com.linkforge.exception.UrlNotFoundException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Dashboard Analytics", description = "Dashboard-oriented analytics endpoints")
class DashboardAnalyticsController(
    private val clickEventRepository: ClickEventRepository,
    private val urlRepository: UrlRepository
) {

    private fun validateDates(from: OffsetDateTime, to: OffsetDateTime) {
        require(from.isBefore(to) || from.isEqual(to)) { "The 'from' date must be before or equal to the 'to' date." }
    }

    @GetMapping("/overview")
    @Operation(summary = "Get analytics overview", description = "Returns total clicks, unique visitors, active URLs, and total URLs")
    fun getOverview(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: OffsetDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: OffsetDateTime
    ): ResponseEntity<DashboardOverviewResponse> {
        validateDates(from, to)

        val totalClicks = clickEventRepository.countClicksBetween(from, to)
        val uniqueVisitors = clickEventRepository.countUniqueVisitorsBetween(from, to)
        val activeUrls = clickEventRepository.countActiveUrlsBetween(from, to)
        val totalUrls = urlRepository.countUrlsCreatedBefore(to)

        return ResponseEntity.ok(DashboardOverviewResponse(totalClicks, uniqueVisitors, activeUrls, totalUrls))
    }

    @GetMapping("/performance")
    @Operation(summary = "Get URL performance", description = "Returns paginated list of top URLs by clicks")
    fun getPerformance(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: OffsetDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: OffsetDateTime,
        pageable: Pageable
    ): ResponseEntity<Page<UrlPerformanceDto>> {
        validateDates(from, to)

        val page = urlRepository.getTopUrlsByClicks(from, to, pageable)
        val dtos = page.map { UrlPerformanceDto(it.urlId, it.shortCode, it.originalUrl, it.clicks) }

        return ResponseEntity.ok(dtos)
    }

    @GetMapping("/trends")
    @Operation(summary = "Get click trends", description = "Returns click trends grouped by hourly, daily, or weekly intervals")
    fun getTrends(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: OffsetDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: OffsetDateTime,
        @RequestParam(defaultValue = "daily") interval: String
    ): ResponseEntity<List<ClickTrendDto>> {
        validateDates(from, to)

        val rawTrends = when (interval.lowercase()) {
            "hourly" -> clickEventRepository.getHourlyTrends(from, to)
            "weekly" -> clickEventRepository.getWeeklyTrends(from, to)
            else -> clickEventRepository.getDailyTrends(from, to)
        }

        val trends = rawTrends.map { row ->
            ClickTrendDto(row[0].toString(), (row[1] as Number).toLong())
        }

        return ResponseEntity.ok(trends)
    }

    @GetMapping("/urls/{shortCode}")
    @Operation(summary = "Get detailed URL analytics", description = "Returns clicks over time, referrers, browsers, and devices for a specific URL")
    fun getUrlAnalytics(
        @PathVariable shortCode: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: OffsetDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: OffsetDateTime,
        @RequestParam(defaultValue = "daily") interval: String,
        @RequestParam(required = false) domain: String?
    ): ResponseEntity<UrlDetailedAnalyticsResponse> {
        validateDates(from, to)

        val url = if (domain.isNullOrBlank()) {
            urlRepository.findByShortCodeAndDomainIsNull(shortCode)
        } else {
            urlRepository.findByShortCodeAndDomain_Domain(shortCode, domain)
        } ?: throw UrlNotFoundException("Short URL not found for code: $shortCode and domain: $domain")

        val totalClicks = clickEventRepository.countClicksByUrlIdBetween(url.id, from, to)

        val rawTrends = when (interval.lowercase()) {
            "hourly" -> clickEventRepository.getUrlHourlyTrends(url.id, from, to)
            "weekly" -> clickEventRepository.getUrlWeeklyTrends(url.id, from, to)
            else -> clickEventRepository.getUrlDailyTrends(url.id, from, to)
        }
        val clicksOverTime = rawTrends.map { row ->
            ClickTrendDto(row[0].toString(), (row[1] as Number).toLong())
        }

        val referrers = clickEventRepository.getReferrers(url.id, from, to).associate { row ->
            row[0].toString() to (row[1] as Number).toLong()
        }

        val browsers = clickEventRepository.getBrowsers(url.id, from, to).associate { row ->
            row[0].toString() to (row[1] as Number).toLong()
        }

        val devices = clickEventRepository.getDevices(url.id, from, to).associate { row ->
            row[0].toString() to (row[1] as Number).toLong()
        }

        val response = UrlDetailedAnalyticsResponse(
            totalClicks = totalClicks,
            clicksOverTime = clicksOverTime,
            referrers = referrers,
            browsers = browsers,
            devices = devices
        )

        return ResponseEntity.ok(response)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<com.linkforge.exception.ErrorResponse> {
        val errorResponse = com.linkforge.exception.ErrorResponse(
            status = org.springframework.http.HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = e.message ?: "Invalid parameters"
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }
}
