package com.linkforge.service.generator

import com.linkforge.model.Url

interface ShortCodeGenerator {
    /**
     * Generates a short code and saves the URL entity for the given original URL.
     * Implementations can use the provided originalUrl, or ignore it.
     */
    fun generate(originalUrl: String, expiresAt: java.time.OffsetDateTime? = null): Url
}
