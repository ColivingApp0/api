package com.coliving.api.identity.application.usecase

import com.coliving.api.identity.application.dto.DocumentSummary
import com.coliving.api.identity.application.dto.ReviewAction
import com.coliving.api.identity.application.dto.ReviewDocumentCommand
import com.coliving.api.identity.domain.enums.DocumentStatus
import com.coliving.api.identity.domain.model.VerificationDocument
import com.coliving.api.identity.domain.repository.VerificationDocumentRepository
import com.coliving.api.shared.error.InvalidArgumentException
import com.coliving.api.shared.error.NotFoundException
import java.time.Clock
import org.springframework.stereotype.Service

/**
 * RF-012 / RF-014: moderation decides on pending documents. The verification
 * level is a derived projection (UserVerificationQueryService), so approving a
 * document needs no profile write.
 */
@Service
class ReviewVerificationDocumentService(
    private val verificationDocumentRepository: VerificationDocumentRepository,
    private val clock: Clock,
) {

    fun listByStatus(status: DocumentStatus?): List<DocumentSummary> {
        val documents = if (status != null) {
            verificationDocumentRepository.findByStatus(status)
        } else {
            verificationDocumentRepository.findAll()
        }
        return documents.map { it.toSummary() }
    }

    fun review(command: ReviewDocumentCommand): DocumentSummary {
        val document = verificationDocumentRepository.findById(command.documentId)
            ?: throw NotFoundException("Verification document not found")

        val now = clock.instant()
        when (command.action) {
            ReviewAction.APPROVE -> document.approve(command.reviewerUserId, now)
            ReviewAction.REJECT -> document.reject(
                command.reviewerUserId,
                command.reason ?: throw InvalidArgumentException("A rejection reason is required"),
                now,
            )

            ReviewAction.REQUEST_CORRECTION -> document.requestCorrection(
                command.reviewerUserId,
                command.reason ?: throw InvalidArgumentException("A correction reason is required"),
                now,
            )
        }
        verificationDocumentRepository.save(document)

        return document.toSummary()
    }

    private fun VerificationDocument.toSummary(): DocumentSummary =
        DocumentSummary(
            id = id,
            type = type,
            status = status,
            uploadedAt = uploadedAt,
            reviewedAt = reviewedAt,
            reviewReason = reviewReason,
        )
}