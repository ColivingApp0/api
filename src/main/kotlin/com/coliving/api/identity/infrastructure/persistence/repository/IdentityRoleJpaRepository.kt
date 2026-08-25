package com.coliving.api.identity.infrastructure.persistence.repository

import com.coliving.api.identity.domain.enums.RoleCode
import com.coliving.api.identity.infrastructure.persistence.entity.IdentityRoleEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface IdentityRoleJpaRepository : JpaRepository<IdentityRoleEntity, UUID> {
    fun findByCode(code: RoleCode): IdentityRoleEntity?
    fun findByActiveTrue(): List<IdentityRoleEntity>
}