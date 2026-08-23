package com.coliving.api.identity.domain.model

import com.coliving.api.identity.domain.enums.DocumentStatus
import com.coliving.api.identity.domain.enums.DocumentType
import java.time.Instant
import java.util.UUID

/**
 * Maps the diagram's `DocumentoVerificacion` entity (RF-012 / RF-014).
 * The reviewer is referenced through [reviewerUserId] (another aggregate),
 * never as a JPA relationship.
 */
class VerificationDocument private constructor(
    val id: UUID,
    val userId: UUID,
    val type: DocumentType,
    val storageKey: String,
    status: DocumentStatus,
    val uploadedAt: Instant,
    reviewerUserId: UUID?,
    reviewReason: String?,
    reviewedAt: Instant?,
) {
    var status: DocumentStatus = status
        private set

    var reviewerUserId: UUID? = reviewerUserId
        private set

    var reviewReason: String? = reviewReason
        private set

    var reviewedAt: Instant? = reviewedAt
        private set

    fun approve(reviewerId: UUID, now: Instant) {
        requirePending()
        status = DocumentStatus.APROBADO
        reviewerUserId = reviewerId
        reviewReason = null
        reviewedAt = now
    }

    fun reject(reviewerId: UUID, reason: String, now: Instant) {
        requirePending()
        status = DocumentStatus.RECHAZADO
        reviewerUserId = reviewerId
        reviewReason = reason.trim().takeIf { it.isNotEmpty() }
        reviewedAt = now
    }

    fun requestCorrection(reviewerId: UUID, reason: String, now: Instant) {
        requirePending()
        status = DocumentStatus.CORRECCION_SOLICITADA
        reviewerUserId = reviewerId
        reviewReason = reason.trim().takeIf { it.isNotEmpty() }
        reviewedAt = now
    }

    private fun requirePending() {
        require(status == DocumentStatus.PENDIENTE) {
            "Document ${type.name} is not pending (current status = $status)"
        }
    }

    companion object {
        fun submit(userId: UUID, type: DocumentType, storageKey: String, now: Instant): VerificationDocument =
            VerificationDocument(
                id = UUID.randomUUID(),
                userId = userId,
                type = type,
                storageKey = storageKey,
                status = DocumentStatus.PENDIENTE,
                uploadedAt = now,
                reviewerUserId = null,
                reviewReason = null,
                reviewedAt = null,
            )

        fun restore(
            id: UUID,
            userId: UUID,
            type: DocumentType,
            storageKey: String,
            status: DocumentStatus,
            uploadedAt: Instant,
            reviewerUserId: UUID?,
            reviewReason: String?,
            reviewedAt: Instant?,
        ): VerificationDocument =
            VerificationDocument(
                id, userId, type, storageKey, status, uploadedAt,
                reviewerUserId, reviewReason, reviewedAt,
            )
    }
}