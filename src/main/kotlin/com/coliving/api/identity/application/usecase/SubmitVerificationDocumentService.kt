package com.coliving.api.identity.application.usecase

import com.coliving.api.identity.application.dto.DocumentSummary
import com.coliving.api.identity.application.dto.SubmitDocumentCommand
import com.coliving.api.identity.application.port.out.DocumentStorage
import com.coliving.api.identity.domain.model.VerificationDocument
import com.coliving.api.identity.domain.repository.VerificationDocumentRepository
import com.coliving.api.shared.error.InvalidArgumentException
import java.time.Clock
import org.springframework.stereotype.Service

/**
 * RF-012: the user uploads a verification document. The file is persisted
 * outside the public web root by [DocumentStorage] and the record starts in
 * PENDIENTE. The user may keep exploring while the review is pending.
 */
@Service
class SubmitVerificationDocumentService(
    private val verificationDocumentRepository: VerificationDocumentRepository,
    private val documentStorage: DocumentStorage,
    private val clock: Clock,
) {

    private val allowedContentTypes = setOf(
        "image/jpeg",
        "image/png",
        "image/webp",
        "application/pdf",
    )

    fun submit(command: SubmitDocumentCommand): DocumentSummary {
        if (command.contentType !in allowedContentTypes) {
            throw InvalidArgumentException("Unsupported file type: ${command.contentType}")
        }
        if (command.bytes.isEmpty()) {
            throw InvalidArgumentException("File is empty")
        }

        val storageKey = documentStorage.store(
            userId = command.userId,
            originalName = command.fileName,
            contentType = command.contentType,
            bytes = command.bytes,
        )
        val document = VerificationDocument.submit(
            userId = command.userId,
            type = command.type,
            storageKey = storageKey,
            now = clock.instant(),
        )
        verificationDocumentRepository.save(document)

        return DocumentSummary(
            id = document.id,
            type = document.type,
            status = document.status,
            uploadedAt = document.uploadedAt,
            reviewedAt = null,
            reviewReason = null,
        )
    }
}