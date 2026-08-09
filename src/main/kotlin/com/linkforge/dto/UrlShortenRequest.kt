package com.linkforge.dto

import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.URL

import jakarta.validation.constraints.Future
import java.time.OffsetDateTime

data class UrlShortenRequest(
    @field:NotBlank(message = "URL cannot be blank")
    @field:URL(message = "Invalid URL format")
    val originalUrl: String,

    @field:Future(message = "Expiration must be in the future")
    val expiresAt: OffsetDateTime? = null
)
