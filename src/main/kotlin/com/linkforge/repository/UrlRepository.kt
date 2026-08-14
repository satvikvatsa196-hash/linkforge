package com.linkforge.repository

import com.linkforge.model.Url
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UrlRepository : JpaRepository<Url, Long> {
    fun findByShortCodeAndDomainIsNull(shortCode: String): Url?
    fun findByShortCodeAndDomain_Domain(shortCode: String, domain: String): Url?
    fun findByShortCode(shortCode: String): Url?
    fun findByOriginalUrlAndDomainIsNull(originalUrl: String): Url?
    fun findByOriginalUrlAndDomain_Domain(originalUrl: String, domain: String): Url?
    fun findByOriginalUrl(originalUrl: String): Url?
    fun findByInactiveFalseAndExpiresAtBefore(date: java.time.OffsetDateTime): List<Url>

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

    @org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(*) FROM urls WHERE created_at <= :to", nativeQuery = true)
    fun countUrlsCreatedBefore(@org.springframework.data.repository.query.Param("to") to: java.time.OffsetDateTime): Long

    @org.springframework.data.jpa.repository.Query(
        value = """
            SELECT u.id as urlId, u.short_code as shortCode, u.original_url as originalUrl, COUNT(c.id) as clicks
            FROM urls u
            JOIN click_events c ON u.id = c.url_id
            WHERE c.clicked_at >= :from AND c.clicked_at <= :to
            GROUP BY u.id, u.short_code, u.original_url
            ORDER BY clicks DESC
        """,
        countQuery = """
            SELECT COUNT(DISTINCT u.id) 
            FROM urls u 
            JOIN click_events c ON u.id = c.url_id 
            WHERE c.clicked_at >= :from AND c.clicked_at <= :to
        """,
        nativeQuery = true
    )
    fun getTopUrlsByClicks(
        @org.springframework.data.repository.query.Param("from") from: java.time.OffsetDateTime,
        @org.springframework.data.repository.query.Param("to") to: java.time.OffsetDateTime,
        pageable: org.springframework.data.domain.Pageable
    ): org.springframework.data.domain.Page<com.linkforge.dto.UrlPerformanceProjection>
}
