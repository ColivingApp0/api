package com.coliving.api.accommodation.infrastructure.provider

import com.coliving.api.accommodation.application.port.out.PublicationReportCasePort
import com.coliving.api.moderation.application.port.out.CaseIntake
import com.coliving.api.moderation.domain.enums.CasePriority
import com.coliving.api.moderation.domain.enums.CaseSubjectType
import com.coliving.api.moderation.domain.enums.CaseType
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Adapter over moderation's [CaseIntake]: a publication report (RF-025) ends
 * in the same auditable support queue as the other reports (RF-082).
 */
@Component
class ModerationPublicationCaseProvider(
    private val caseIntake: CaseIntake,
) : PublicationReportCasePort {

    override fun openPublicationReportCase(
        publicationId: UUID,
        reportedUserId: UUID,
        reporterUserId: UUID,
        reason: String,
    ): UUID = caseIntake.openCase(
        type = CaseType.REPORTE,
        subjectType = CaseSubjectType.PUBLICACION,
        subjectId = publicationId,
        reportedUserId = reportedUserId,
        openedByUserId = reporterUserId,
        description = reason,
        priority = CasePriority.MEDIA,
    )
}