package com.linkforge.model

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "domains")
class Domain(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "domain", nullable = false, unique = true, length = 255)
    var domain: String,

    @Column(name = "created_at", updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "active")
    var active: Boolean = true
)
