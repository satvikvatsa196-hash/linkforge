package com.linkforge.model

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "urls")
class Url(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "original_url", nullable = false, length = 2048)
    var originalUrl: String,

    @Column(name = "short_code", nullable = false, unique = true, length = 50)
    var shortCode: String,

    @Column(name = "created_at", updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "clicks_count")
    var clicksCount: Long = 0
)
