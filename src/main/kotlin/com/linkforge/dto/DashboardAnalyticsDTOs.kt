package com.linkforge.dto

data class DashboardOverviewResponse(
    val totalClicks: Long,
    val uniqueVisitors: Long,
    val activeUrls: Long,
    val totalUrls: Long
)

interface UrlPerformanceProjection {
    val urlId: Long
    val shortCode: String
    val originalUrl: String
    val clicks: Long
}

data class UrlPerformanceDto(
    val urlId: Long,
    val shortCode: String,
    val originalUrl: String,
    val clicks: Long
)

data class ClickTrendDto(
    val timestamp: String,
    val clicks: Long
)

data class UrlDetailedAnalyticsResponse(
    val totalClicks: Long,
    val clicksOverTime: List<ClickTrendDto>,
    val referrers: Map<String, Long>,
    val browsers: Map<String, Long>,
    val devices: Map<String, Long>
)
