package com.linkforge.dto

import java.time.OffsetDateTime

data class ClickEventMessage(
    val urlId: Long,
    val shortCode: String,
    val timestamp: OffsetDateTime,
    val ipHash: String,
    val userAgent: String?,
    val referrer: String?
)
