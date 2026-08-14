package com.linkforge.repository

import com.linkforge.model.ClickEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ClickEventRepository : JpaRepository<ClickEvent, Long> {

    fun countByUrlId(urlId: Long): Long
    
    fun findFirstByUrlIdOrderByClickedAtAsc(urlId: Long): ClickEvent?
    
    fun findFirstByUrlIdOrderByClickedAtDesc(urlId: Long): ClickEvent?
    
    @Query(value = """
        SELECT DATE(clicked_at) as click_date, COUNT(*) as click_count 
        FROM click_events 
        WHERE url_id = :urlId 
        GROUP BY DATE(clicked_at) 
        ORDER BY click_date ASC
    """, nativeQuery = true)
    fun getClicksByDay(@Param("urlId") urlId: Long): List<Array<Any>>

    @Query(value = "SELECT COUNT(*) FROM click_events WHERE url_id = :urlId AND clicked_at >= :from AND clicked_at <= :to", nativeQuery = true)
    fun countClicksByUrlIdBetween(@Param("urlId") urlId: Long, @Param("from") from: java.time.OffsetDateTime, @Param("to") to: java.time.OffsetDateTime): Long


    @Query(value = "SELECT COUNT(*) FROM click_events WHERE clicked_at >= :from AND clicked_at <= :to", nativeQuery = true)
    fun countClicksBetween(@Param("from") from: java.time.OffsetDateTime, @Param("to") to: java.time.OffsetDateTime): Long

    @Query(value = "SELECT COUNT(DISTINCT ip_hash) FROM click_events WHERE clicked_at >= :from AND clicked_at <= :to", nativeQuery = true)
    fun countUniqueVisitorsBetween(@Param("from") from: java.time.OffsetDateTime, @Param("to") to: java.time.OffsetDateTime): Long

    @Query(value = "SELECT COUNT(DISTINCT url_id) FROM click_events WHERE clicked_at >= :from AND clicked_at <= :to", nativeQuery = true)
    fun countActiveUrlsBetween(@Param("from") from: java.time.OffsetDateTime, @Param("to") to: java.time.OffsetDateTime): Long

    @Query(value = """
        SELECT DATE_TRUNC('hour', clicked_at) as period, COUNT(*) as click_count 
        FROM click_events 
        WHERE clicked_at >= :from AND clicked_at <= :to 
        GROUP BY period 
        ORDER BY period ASC
    """, nativeQuery = true)
    fun getHourlyTrends(@Param("from") from: java.time.OffsetDateTime, @Param("to") to: java.time.OffsetDateTime): List<Array<Any>>

    @Query(value = """
        SELECT DATE_TRUNC('day', clicked_at) as period, COUNT(*) as click_count 
        FROM click_events 
        WHERE clicked_at >= :from AND clicked_at <= :to 
        GROUP BY period 
        ORDER BY period ASC
    """, nativeQuery = true)
    fun getDailyTrends(@Param("from") from: java.time.OffsetDateTime, @Param("to") to: java.time.OffsetDateTime): List<Array<Any>>

    @Query(value = """
        SELECT DATE_TRUNC('week', clicked_at) as period, COUNT(*) as click_count 
        FROM click_events 
        WHERE clicked_at >= :from AND clicked_at <= :to 
        GROUP BY period 
        ORDER BY period ASC
    """, nativeQuery = true)
    fun getWeeklyTrends(@Param("from") from: java.time.OffsetDateTime, @Param("to") to: java.time.OffsetDateTime): List<Array<Any>>

    @Query(value = """
        SELECT DATE_TRUNC('hour', clicked_at) as period, COUNT(*) as click_count 
        FROM click_events 
        WHERE url_id = :urlId AND clicked_at >= :from AND clicked_at <= :to 
        GROUP BY period 
        ORDER BY period ASC
    """, nativeQuery = true)
    fun getUrlHourlyTrends(@Param("urlId") urlId: Long, @Param("from") from: java.time.OffsetDateTime, @Param("to") to: java.time.OffsetDateTime): List<Array<Any>>

    @Query(value = """
        SELECT DATE_TRUNC('day', clicked_at) as period, COUNT(*) as click_count 
        FROM click_events 
        WHERE url_id = :urlId AND clicked_at >= :from AND clicked_at <= :to 
        GROUP BY period 
        ORDER BY period ASC
    """, nativeQuery = true)
    fun getUrlDailyTrends(@Param("urlId") urlId: Long, @Param("from") from: java.time.OffsetDateTime, @Param("to") to: java.time.OffsetDateTime): List<Array<Any>>

    @Query(value = """
        SELECT DATE_TRUNC('week', clicked_at) as period, COUNT(*) as click_count 
        FROM click_events 
        WHERE url_id = :urlId AND clicked_at >= :from AND clicked_at <= :to 
        GROUP BY period 
        ORDER BY period ASC
    """, nativeQuery = true)
    fun getUrlWeeklyTrends(@Param("urlId") urlId: Long, @Param("from") from: java.time.OffsetDateTime, @Param("to") to: java.time.OffsetDateTime): List<Array<Any>>

    @Query(value = """
        SELECT COALESCE(referrer, 'Direct') as ref, COUNT(*) as click_count 
        FROM click_events 
        WHERE url_id = :urlId AND clicked_at >= :from AND clicked_at <= :to 
        GROUP BY ref 
        ORDER BY click_count DESC
    """, nativeQuery = true)
    fun getReferrers(@Param("urlId") urlId: Long, @Param("from") from: java.time.OffsetDateTime, @Param("to") to: java.time.OffsetDateTime): List<Array<Any>>

    @Query(value = """
        SELECT 
          CASE 
            WHEN user_agent LIKE '%Chrome%' THEN 'Chrome'
            WHEN user_agent LIKE '%Firefox%' THEN 'Firefox'
            WHEN user_agent LIKE '%Safari%' AND user_agent NOT LIKE '%Chrome%' THEN 'Safari'
            WHEN user_agent LIKE '%Edge%' OR user_agent LIKE '%Edg/%' THEN 'Edge'
            ELSE 'Other'
          END as browser,
          COUNT(*) as click_count
        FROM click_events
        WHERE url_id = :urlId AND clicked_at >= :from AND clicked_at <= :to
        GROUP BY browser
        ORDER BY click_count DESC
    """, nativeQuery = true)
    fun getBrowsers(@Param("urlId") urlId: Long, @Param("from") from: java.time.OffsetDateTime, @Param("to") to: java.time.OffsetDateTime): List<Array<Any>>

    @Query(value = """
        SELECT 
          CASE 
            WHEN user_agent LIKE '%Mobile%' OR user_agent LIKE '%Android%' OR user_agent LIKE '%iPhone%' THEN 'Mobile'
            WHEN user_agent LIKE '%iPad%' OR user_agent LIKE '%Tablet%' THEN 'Tablet'
            ELSE 'Desktop'
          END as device,
          COUNT(*) as click_count
        FROM click_events
        WHERE url_id = :urlId AND clicked_at >= :from AND clicked_at <= :to
        GROUP BY device
        ORDER BY click_count DESC
    """, nativeQuery = true)
    fun getDevices(@Param("urlId") urlId: Long, @Param("from") from: java.time.OffsetDateTime, @Param("to") to: java.time.OffsetDateTime): List<Array<Any>>
}
