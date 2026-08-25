package com.coliving.api.identity.infrastructure.persistence.repository

import com.coliving.api.identity.infrastructure.persistence.entity.IdentityUserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface IdentityUserJpaRepository : JpaRepository<IdentityUserEntity, UUID> {
    fun findByEmail(email: String): IdentityUserEntity?
    fun existsByEmail(email: String): Boolean
}