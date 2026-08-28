package com.coliving.api.identity.infrastructure.persistence.entity

import com.coliving.api.identity.domain.enums.DocumentStatus
import com.coliving.api.identity.domain.enums.DocumentType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "identity_verification_document",
    indexes = [
        Index(name = "idx_identity_verification_document_status", columnList = "status"),
        Index(name = "idx_identity_verification_document_user", columnList = "user_id"),
    ],
)
class IdentityVerificationDocumentEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    var documentType: DocumentType = DocumentType.IDENTIDAD,

    @Column(name = "storage_key", nullable = false, length = 500)
    var storageKey: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    var status: DocumentStatus = DocumentStatus.PENDIENTE,

    @Column(name = "review_reason", length = 1000)
    var reviewReason: String? = null,

    /** Reference to the reviewer user (another aggregate, no JPA relation). */
    @Column(name = "reviewer_user_id")
    var reviewerUserId: UUID? = null,

    @Column(name = "uploaded_at", nullable = false)
    var uploadedAt: Instant = Instant.now(),

    @Column(name = "reviewed_at")
    var reviewedAt: Instant? = null,
)