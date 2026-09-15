package com.coliving.api.moderation.domain.model

import com.coliving.api.moderation.domain.enums.CasePriority
import com.coliving.api.moderation.domain.enums.CaseStatus
import com.coliving.api.moderation.domain.enums.CaseSubjectType
import com.coliving.api.moderation.domain.enums.CaseType
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.InvalidArgumentException
import java.time.Instant
import java.util.UUID

/**
 * Support case (RF-082): a report or a dispute that the support team handles
 * with states, priority and a responsible moderator.
 *
 * The case is the single artifact shared by `messaging` (a reported
 * conversation, RF-052) and `reputation` (a disputed evaluation, RF-072), so
 * both flows end in the same auditable queue.
 */
class SupportCase(
    val id: UUID,
    val type: CaseType,
    val subjectType: CaseSubjectType,
    val subjectId: UUID,
    val reportedUserId: UUID?,
    val openedByUserId: UUID?,
    val description: String,
    var priority: CasePriority,
    var status: CaseStatus,
    var assigneeUserId: UUID?,
    var resolution: String?,
    val openedAt: Instant,
    var updatedAt: Instant,
    var closedAt: Instant?,
) {

    fun isOpen(): Boolean = status == CaseStatus.ABIERTO || status == CaseStatus.EN_REVISION

    /** Fails unless the case still admits decisions (terminal states are final). */
    fun requireOpen() {
        if (!isOpen()) {
            throw ConflictException("The case is already closed")
        }
    }

    /** Assigns (or reassigns) the responsible moderator. */
    fun assign(moderatorId: UUID, now: Instant) {
        requireOpen()
        assigneeUserId = moderatorId
        updatedAt = now
    }

    /** Moves the case into review: support acknowledged and started working. */
    fun startReview(now: Instant) {
        requireOpen()
        status = CaseStatus.EN_REVISION
        updatedAt = now
    }

    /** Resolves the case; the resolution note is mandatory and auditable. */
    fun resolve(resolution: String, now: Instant) {
        val note = requireNote(resolution, "A resolution note is required")
        status = CaseStatus.RESUELTO
        this.resolution = note
        updatedAt = now
        closedAt = now
    }

    /** Rejects the case (no action taken); the reason is mandatory. */
    fun reject(reason: String, now: Instant) {
        val note = requireNote(reason, "A rejection reason is required")
        status = CaseStatus.RECHAZADO
        resolution = note
        updatedAt = now
        closedAt = now
    }

    /**
     * Response time in hours, derived from the stored instants (RF-082: "tiempos
     * de atención medibles"). Null while the case is still open.
     */
    fun resolutionHours(): Double? =
        closedAt?.let { (it.toEpochMilli() - openedAt.toEpochMilli()) / 3_600_000.0 }

    private fun requireNote(value: String, message: String): String {
        requireOpen()
        val note = value.trim()
        if (note.isEmpty()) {
            throw InvalidArgumentException(message)
        }
        if (note.length > MAX_NOTE_LENGTH) {
            throw InvalidArgumentException("The note must not exceed $MAX_NOTE_LENGTH characters")
        }
        return note
    }

    companion object {
        const val MAX_DESCRIPTION_LENGTH = 1000
        const val MAX_NOTE_LENGTH = 1000

        /**
         * Opens a case. The description is mandatory so the moderator always has
         * the reporter's account of the facts (RF-082 traceability).
         */
        fun open(
            id: UUID,
            type: CaseType,
            subjectType: CaseSubjectType,
            subjectId: UUID,
            reportedUserId: UUID?,
            openedByUserId: UUID?,
            description: String,
            priority: CasePriority,
            now: Instant,
        ): SupportCase {
            val normalized = description.trim()
            if (normalized.isEmpty()) {
                throw InvalidArgumentException("A description is required to open a case")
            }
            if (normalized.length > MAX_DESCRIPTION_LENGTH) {
                throw InvalidArgumentException(
                    "The description must not exceed $MAX_DESCRIPTION_LENGTH characters",
                )
            }
            if (reportedUserId != null && reportedUserId == openedByUserId) {
                throw InvalidArgumentException("A user cannot report or dispute against themselves")
            }
            return SupportCase(
                id = id,
                type = type,
                subjectType = subjectType,
                subjectId = subjectId,
                reportedUserId = reportedUserId,
                openedByUserId = openedByUserId,
                description = normalized,
                priority = priority,
                status = CaseStatus.ABIERTO,
                assigneeUserId = null,
                resolution = null,
                openedAt = now,
                updatedAt = now,
                closedAt = null,
            )
        }
    }
}