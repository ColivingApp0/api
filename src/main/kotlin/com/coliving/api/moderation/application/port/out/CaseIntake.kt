package com.coliving.api.moderation.application.port.out

import com.coliving.api.moderation.domain.enums.CasePriority
import com.coliving.api.moderation.domain.enums.CaseSubjectType
import com.coliving.api.moderation.domain.enums.CaseType
import java.util.UUID

/**
 * Case intake contract exposed by moderation to the contexts that need to raise
 * a case: `messaging` reports a conversation (RF-052) and `reputation` disputes
 * an evaluation (RF-072). Same cross-context pattern as accommodation's
 * ReservationAvailabilityPort: the providing context owns the interface and its
 * implementation, the consumer adapts it to its own port.
 *
 * The command carries no domain type from the caller beyond plain values, so
 * moderation stays independent of the reporting contexts.
 */
interface CaseIntake {

    /** Opens a case and returns its id, which the caller stores as a reference. */
    fun openCase(
        type: CaseType,
        subjectType: CaseSubjectType,
        subjectId: UUID,
        reportedUserId: UUID?,
        openedByUserId: UUID?,
        description: String,
        priority: CasePriority,
    ): UUID
}