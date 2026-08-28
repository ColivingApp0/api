package com.coliving.api.identity.infrastructure.persistence.repository

import com.coliving.api.identity.infrastructure.persistence.entity.IdentityUserRoleEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface IdentityUserRoleJpaRepository : JpaRepository<IdentityUserRoleEntity, UUID> {
    fun findByUserId(userId: UUID): List<IdentityUserRoleEntity>
}