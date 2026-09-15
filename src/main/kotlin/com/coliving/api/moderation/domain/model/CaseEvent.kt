package com.coliving.api.moderation.domain.model

import com.coliving.api.moderation.domain.enums.CaseEventType
import com.coliving.api.shared.error.InvalidArgumentException
import java.time.Instant
import java.util.UUID

/**
 * Traceability entry of a case (RF-082): every action records who did it, when
 * and with which note, so a case can be reconstructed end to end.
 */
class CaseEvent(
    val id: UUID,
    val caseId: UUID,
    val type: CaseEventType,
    val actorUserId: UUID,
    val note: String?,
    val occurredAt: Instant,
) {

    companion object {
        const val MAX_NOTE_LENGTH = 1000

        fun record(
            id: UUID,
            caseId: UUID,
            type: CaseEventType,
            actorUserId: UUID,
            note: String?,
            now: Instant,
        ): CaseEvent {
            val normalized = note?.trim()?.takeIf { it.isNotEmpty() }
            if (normalized != null && normalized.length > MAX_NOTE_LENGTH) {
                throw InvalidArgumentException("The note must not exceed $MAX_NOTE_LENGTH characters")
            }
            return CaseEvent(
                id = id,
                caseId = caseId,
                type = type,
                actorUserId = actorUserId,
                note = normalized,
                occurredAt = now,
            )
        }
    }
}