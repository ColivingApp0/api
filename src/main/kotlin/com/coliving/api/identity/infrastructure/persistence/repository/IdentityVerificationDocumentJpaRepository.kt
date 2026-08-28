package com.coliving.api.identity.infrastructure.persistence.repository

import com.coliving.api.identity.domain.enums.DocumentStatus
import com.coliving.api.identity.infrastructure.persistence.entity.IdentityVerificationDocumentEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface IdentityVerificationDocumentJpaRepository : JpaRepository<IdentityVerificationDocumentEntity, UUID> {
    fun findByUserIdOrderByUploadedAtDesc(userId: UUID): List<IdentityVerificationDocumentEntity>
    fun findByStatusOrderByUploadedAtDesc(status: DocumentStatus): List<IdentityVerificationDocumentEntity>
    fun findAllByOrderByUploadedAtDesc(): List<IdentityVerificationDocumentEntity>
}