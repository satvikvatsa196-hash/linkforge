package com.linkforge.dto

import java.time.OffsetDateTime

data class UrlShortenResponse(
    val shortCode: String,
    val originalUrl: String,
    val shortUrl: String, // Full short URL like http://localhost:8080/{shortCode}
    val createdAt: OffsetDateTime,
    val expiresAt: OffsetDateTime? = null
)
