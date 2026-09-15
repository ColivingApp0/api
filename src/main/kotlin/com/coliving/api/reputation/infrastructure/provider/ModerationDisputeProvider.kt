package com.coliving.api.reputation.infrastructure.provider

import com.coliving.api.moderation.application.port.out.CaseIntake
import com.coliving.api.moderation.domain.enums.CasePriority
import com.coliving.api.moderation.domain.enums.CaseSubjectType
import com.coliving.api.moderation.domain.enums.CaseType
import com.coliving.api.reputation.application.port.out.DisputeCasePort
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Adapter over moderation's [CaseIntake]: disputing an evaluation creates a case
 * in the same support queue the team already uses (RF-072). A dispute is
 * triaged as high priority because it suspends a published score.
 */
@Component
class ModerationDisputeProvider(
    private val caseIntake: CaseIntake,
) : DisputeCasePort {

    override fun openEvaluationDisputeCase(
        evaluationId: UUID,
        evaluatedUserId: UUID,
        disputantUserId: UUID,
        reason: String,
    ): UUID = caseIntake.openCase(
        type = CaseType.DISPUTA,
        subjectType = CaseSubjectType.EVALUACION,
        subjectId = evaluationId,
        reportedUserId = evaluatedUserId,
        openedByUserId = disputantUserId,
        description = reason,
        priority = CasePriority.ALTA,
    )
}