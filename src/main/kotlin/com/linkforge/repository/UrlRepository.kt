package com.linkforge.repository

import com.linkforge.model.Url
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UrlRepository : JpaRepository<Url, Long> {
    fun findByShortCode(shortCode: String): Url?
    fun findByOriginalUrl(originalUrl: String): Url?
}
