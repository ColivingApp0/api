package com.coliving.api.messaging.domain.model

import com.coliving.api.shared.error.InvalidArgumentException
import java.time.Instant
import java.util.UUID

/**
 * Block between two users (RF-053). While it exists, the blocked counterpart
 * cannot write into the conversations shared with the blocker — the messaging
 * equivalent of "no volver a interactuar con un usuario". Symmetric reads are
 * allowed; writing is what the guard stops.
 */
class UserBlock(
    val blockerUserId: UUID,
    val blockedUserId: UUID,
    val createdAt: Instant,
) {

    companion object {
        fun create(blockerUserId: UUID, blockedUserId: UUID, now: Instant): UserBlock {
            if (blockerUserId == blockedUserId) {
                throw InvalidArgumentException("A user cannot block themselves")
            }
            return UserBlock(blockerUserId, blockedUserId, now)
        }
    }
}

/**
 * Report of a conversation (RF-052). Opening it also raises a support case in
 * `moderation` (the shared auditable queue) and the returned case id is stored
 * so the reporter can follow up on the outcome.
 */
class ConversationReport(
    val id: UUID,
    val conversationId: UUID,
    val reporterUserId: UUID,
    val reportedUserId: UUID,
    var reason: String,
    var caseId: UUID,
    val createdAt: Instant,
) {

    companion object {
        const val MAX_REASON_LENGTH = 1000

        fun create(
            id: UUID,
            conversationId: UUID,
            reporterUserId: UUID,
            reportedUserId: UUID,
            reason: String,
            caseId: UUID,
            now: Instant,
        ): ConversationReport {
            if (reporterUserId == reportedUserId) {
                throw InvalidArgumentException("A user cannot report themselves")
            }
            val normalized = reason.trim()
            if (normalized.isEmpty()) {
                throw InvalidArgumentException("A reason is required to report a conversation")
            }
            if (normalized.length > MAX_REASON_LENGTH) {
                throw InvalidArgumentException("The reason must not exceed $MAX_REASON_LENGTH characters")
            }
            return ConversationReport(
                id = id,
                conversationId = conversationId,
                reporterUserId = reporterUserId,
                reportedUserId = reportedUserId,
                reason = normalized,
                caseId = caseId,
                createdAt = now,
            )
        }
    }
}