package com.coliving.api.identity.domain

import com.coliving.api.identity.domain.enums.DocumentStatus
import com.coliving.api.identity.domain.enums.DocumentType
import com.coliving.api.identity.domain.model.VerificationDocument
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VerificationDocumentTest {

    private val now: Instant = Instant.parse("2026-09-01T10:00:00Z")
    private val userId = UUID.randomUUID()
    private val reviewerId = UUID.randomUUID()

    private fun pendingDocument(): VerificationDocument =
        VerificationDocument.submit(userId, DocumentType.IDENTIDAD, "user/dni.pdf", now)

    @Test
    fun `submit creates a pending document`() {
        val document = pendingDocument()

        assertEquals(DocumentStatus.PENDIENTE, document.status)
        assertEquals(DocumentType.IDENTIDAD, document.type)
    }

    @Test
    fun `approval records reviewer and timestamp`() {
        val document = pendingDocument()

        document.approve(reviewerId, now.plusSeconds(10))

        assertEquals(DocumentStatus.APROBADO, document.status)
        assertEquals(reviewerId, document.reviewerUserId)
        assertEquals(now.plusSeconds(10), document.reviewedAt)
    }

    @Test
    fun `rejection requires a reason field and stores it`() {
        val document = pendingDocument()

        document.reject(reviewerId, "Documento ilegible", now.plusSeconds(10))

        assertEquals(DocumentStatus.RECHAZADO, document.status)
        assertEquals("Documento ilegible", document.reviewReason)
    }

    @Test
    fun `finalized document cannot be reviewed again`() {
        val document = pendingDocument()
        document.approve(reviewerId, now.plusSeconds(10))

        val result = kotlin.runCatching { document.reject(reviewerId, "too late", now.plusSeconds(20)) }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(DocumentStatus.APROBADO, document.status)
        assertFalse(document.reviewReason != null)
    }
}