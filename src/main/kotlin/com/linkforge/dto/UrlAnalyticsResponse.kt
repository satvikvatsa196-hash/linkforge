package com.linkforge.dto

import java.time.OffsetDateTime

data class UrlAnalyticsResponse(
    val totalClicks: Long,
    val firstClick: OffsetDateTime?,
    val lastClick: OffsetDateTime?,
    val clicksByDay: Map<String, Long>
)
