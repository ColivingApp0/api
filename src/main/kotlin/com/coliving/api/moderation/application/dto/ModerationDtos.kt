package com.coliving.api.moderation.application.dto

import com.coliving.api.moderation.domain.enums.CaseEventType
import com.coliving.api.moderation.domain.enums.CasePriority
import com.coliving.api.moderation.domain.enums.CaseStatus
import com.coliving.api.moderation.domain.enums.CaseSubjectType
import com.coliving.api.moderation.domain.enums.CaseType
import java.time.Instant
import java.util.UUID

/** Case as seen in the support queue (RF-082). */
data class CaseView(
    val id: UUID,
    val type: CaseType,
    val subjectType: CaseSubjectType,
    val subjectId: UUID,
    val reportedUserId: UUID?,
    val openedByUserId: UUID?,
    val description: String,
    val priority: CasePriority,
    val status: CaseStatus,
    val assigneeUserId: UUID?,
    val resolution: String?,
    val openedAt: Instant,
    val updatedAt: Instant,
    val closedAt: Instant?,
    /** Measurable response time (RF-082); null while the case is open. */
    val resolutionHours: Double?,
)

/** One traceability entry of a case (RF-082). */
data class CaseEventView(
    val id: UUID,
    val type: CaseEventType,
    val actorUserId: UUID,
    val note: String?,
    val occurredAt: Instant,
)

/** Case plus its full traceability. */
data class CaseDetailView(
    val case: CaseView,
    val events: List<CaseEventView>,
)

/** Filters of the support queue; every filter is optional. */
data class CaseSearchQuery(
    val type: CaseType? = null,
    val status: CaseStatus? = null,
    val priority: CasePriority? = null,
    val assigneeUserId: UUID? = null,
    val onlyOpen: Boolean = false,
)

/** Assign (or reassign) the case to a moderator. */
data class AssignCaseCommand(
    val caseId: UUID,
    val moderatorId: UUID,
)

/** Move the case into review. */
data class StartCaseReviewCommand(
    val caseId: UUID,
    val moderatorId: UUID,
)

/** Close the case as resolved; the resolution is mandatory. */
data class ResolveCaseCommand(
    val caseId: UUID,
    val moderatorId: UUID,
    val resolution: String,
)

/** Close the case as rejected; the reason is mandatory. */
data class RejectCaseCommand(
    val caseId: UUID,
    val moderatorId: UUID,
    val reason: String,
)