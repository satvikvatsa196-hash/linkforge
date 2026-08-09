package com.linkforge.repository

import com.linkforge.model.Url
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UrlRepository : JpaRepository<Url, Long> {
    fun findByShortCode(shortCode: String): Url?
    fun findByOriginalUrl(originalUrl: String): Url?
    fun findByInactiveFalseAndExpiresAtBefore(date: java.time.OffsetDateTime): List<Url>

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Url u SET u.clicksCount = u.clicksCount + 1 WHERE u.shortCode = :shortCode")
    fun incrementClickCount(@org.springframework.data.repository.query.Param("shortCode") shortCode: String)

    @org.springframework.data.jpa.repository.Query(value = "SELECT nextval('urls_id_seq')", nativeQuery = true)
    fun getNextSequenceValue(): Long

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
        value = "INSERT INTO urls (id, original_url, short_code, created_at, updated_at, clicks_count, expires_at, inactive) VALUES (:id, :originalUrl, :shortCode, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, :expiresAt, false)",
        nativeQuery = true
    )
    fun insertUrlWithId(
        @org.springframework.data.repository.query.Param("id") id: Long,
        @org.springframework.data.repository.query.Param("originalUrl") originalUrl: String,
        @org.springframework.data.repository.query.Param("shortCode") shortCode: String,
        @org.springframework.data.repository.query.Param("expiresAt") expiresAt: java.time.OffsetDateTime? = null
    )
}
