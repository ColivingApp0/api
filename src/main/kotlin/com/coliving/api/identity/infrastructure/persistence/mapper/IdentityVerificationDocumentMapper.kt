package com.coliving.api.identity.infrastructure.persistence.mapper

import com.coliving.api.identity.domain.model.VerificationDocument
import com.coliving.api.identity.infrastructure.persistence.entity.IdentityVerificationDocumentEntity

object IdentityVerificationDocumentMapper {

    fun toDomain(entity: IdentityVerificationDocumentEntity): VerificationDocument =
        VerificationDocument.restore(
            id = entity.id,
            userId = entity.userId,
            type = entity.documentType,
            storageKey = entity.storageKey,
            status = entity.status,
            uploadedAt = entity.uploadedAt,
            reviewerUserId = entity.reviewerUserId,
            reviewReason = entity.reviewReason,
            reviewedAt = entity.reviewedAt,
        )

    fun toEntity(document: VerificationDocument): IdentityVerificationDocumentEntity =
        IdentityVerificationDocumentEntity(
            id = document.id,
            userId = document.userId,
            documentType = document.type,
            storageKey = document.storageKey,
            status = document.status,
            reviewReason = document.reviewReason,
            reviewerUserId = document.reviewerUserId,
            uploadedAt = document.uploadedAt,
            reviewedAt = document.reviewedAt,
        )
}