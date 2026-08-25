package com.coliving.api.identity.infrastructure.persistence.adapter

import com.coliving.api.identity.domain.enums.DocumentStatus
import com.coliving.api.identity.domain.model.VerificationDocument
import com.coliving.api.identity.domain.repository.VerificationDocumentRepository
import com.coliving.api.identity.infrastructure.persistence.mapper.IdentityVerificationDocumentMapper
import com.coliving.api.identity.infrastructure.persistence.repository.IdentityVerificationDocumentJpaRepository
import java.util.UUID
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class VerificationDocumentRepositoryAdapter(
    private val documentJpaRepository: IdentityVerificationDocumentJpaRepository,
) : VerificationDocumentRepository {

    override fun findById(id: UUID): VerificationDocument? =
        documentJpaRepository.findById(id).orElse(null)?.let(IdentityVerificationDocumentMapper::toDomain)

    override fun findByUser(userId: UUID): List<VerificationDocument> =
        documentJpaRepository.findByUserIdOrderByUploadedAtDesc(userId)
            .map(IdentityVerificationDocumentMapper::toDomain)

    override fun findByStatus(status: DocumentStatus): List<VerificationDocument> =
        documentJpaRepository.findByStatusOrderByUploadedAtDesc(status)
            .map(IdentityVerificationDocumentMapper::toDomain)

    override fun findAll(): List<VerificationDocument> =
        documentJpaRepository.findAllByOrderByUploadedAtDesc()
            .map(IdentityVerificationDocumentMapper::toDomain)

    @Transactional
    override fun save(document: VerificationDocument) {
        documentJpaRepository.save(IdentityVerificationDocumentMapper.toEntity(document))
    }
}