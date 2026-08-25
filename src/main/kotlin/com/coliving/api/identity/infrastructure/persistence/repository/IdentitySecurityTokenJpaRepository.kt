package com.coliving.api.identity.infrastructure.persistence.repository

import com.coliving.api.identity.domain.enums.SecurityTokenPurpose
import com.coliving.api.identity.infrastructure.persistence.entity.IdentitySecurityTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface IdentitySecurityTokenJpaRepository : JpaRepository<IdentitySecurityTokenEntity, UUID> {
    fun findByTokenHashAndPurpose(
        tokenHash: String,
        purpose: SecurityTokenPurpose,
    ): IdentitySecurityTokenEntity?
}