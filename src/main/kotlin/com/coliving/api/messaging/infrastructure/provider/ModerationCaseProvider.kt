package com.coliving.api.messaging.infrastructure.provider

import com.coliving.api.messaging.application.port.out.ReportCasePort
import com.coliving.api.moderation.application.port.out.CaseIntake
import com.coliving.api.moderation.domain.enums.CasePriority
import com.coliving.api.moderation.domain.enums.CaseSubjectType
import com.coliving.api.moderation.domain.enums.CaseType
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Adapter over moderation's [CaseIntake] (RF-052): reporting a conversation
 * opens a support case in the shared auditable queue, so both messaging
 * reports and reputation disputes end in the same place.
 */
@Component
class ModerationCaseProvider(
    private val caseIntake: CaseIntake,
) : ReportCasePort {

    override fun openConversationReportCase(
        conversationId: UUID,
        reportedUserId: UUID,
        reporterUserId: UUID,
        reason: String,
    ): UUID = caseIntake.openCase(
        type = CaseType.REPORTE,
        subjectType = CaseSubjectType.CONVERSACION,
        subjectId = conversationId,
        reportedUserId = reportedUserId,
        openedByUserId = reporterUserId,
        description = reason,
        priority = CasePriority.MEDIA,
    )
}