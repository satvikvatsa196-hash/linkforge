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
}
