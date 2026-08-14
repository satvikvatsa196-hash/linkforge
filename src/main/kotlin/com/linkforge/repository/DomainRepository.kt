package com.linkforge.repository

import com.linkforge.model.Domain
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DomainRepository : JpaRepository<Domain, Long> {
    fun findByDomain(domain: String): Domain?
}
