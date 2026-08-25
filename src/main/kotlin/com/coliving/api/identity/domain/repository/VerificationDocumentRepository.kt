package com.coliving.api.identity.domain.repository

import com.coliving.api.identity.domain.enums.DocumentStatus
import com.coliving.api.identity.domain.model.VerificationDocument
import java.util.UUID

/**
 * Out port for verification documents (RF-012). Documents are immutable once
 * created; review transitions happen through domain methods before saving.
 */
interface VerificationDocumentRepository {
    fun findById(id: UUID): VerificationDocument?
    fun findByUser(userId: UUID): List<VerificationDocument>
    fun findByStatus(status: DocumentStatus): List<VerificationDocument>
    fun findAll(): List<VerificationDocument>
    fun save(document: VerificationDocument)
}