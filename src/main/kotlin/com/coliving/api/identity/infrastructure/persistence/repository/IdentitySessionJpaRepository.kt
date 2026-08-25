package com.coliving.api.identity.infrastructure.persistence.repository

import com.coliving.api.identity.infrastructure.persistence.entity.IdentitySessionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface IdentitySessionJpaRepository : JpaRepository<IdentitySessionEntity, UUID> {
    fun findByTokenHash(tokenHash: String): IdentitySessionEntity?
    fun findByUserId(userId: UUID): List<IdentitySessionEntity>
}