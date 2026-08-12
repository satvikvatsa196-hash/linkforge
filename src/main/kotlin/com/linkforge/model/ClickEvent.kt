package com.linkforge.model

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "click_events")
class ClickEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "url_id", nullable = false)
    val urlId: Long,

    @Column(name = "clicked_at", nullable = false)
    val clickedAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "ip_hash", nullable = false, length = 64)
    val ipHash: String,

    @Column(name = "user_agent", length = 512)
    val userAgent: String?,

    @Column(name = "referrer", length = 512)
    val referrer: String?
)
