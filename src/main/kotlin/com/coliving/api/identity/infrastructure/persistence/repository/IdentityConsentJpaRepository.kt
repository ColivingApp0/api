package com.coliving.api.identity.infrastructure.persistence.repository

import com.coliving.api.identity.domain.enums.ConsentType
import com.coliving.api.identity.infrastructure.persistence.entity.IdentityConsentEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface IdentityConsentJpaRepository : JpaRepository<IdentityConsentEntity, UUID> {
    fun findByUserIdAndType(userId: UUID, type: ConsentType): IdentityConsentEntity?
    fun findByUserIdOrderByAcceptedAtDesc(userId: UUID): List<IdentityConsentEntity>
}