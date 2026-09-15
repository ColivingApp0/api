package com.coliving.api.moderation.application.usecase

import com.coliving.api.moderation.application.dto.ModerationMetricsView
import com.coliving.api.moderation.domain.enums.CaseStatus
import com.coliving.api.moderation.domain.enums.CaseSubjectType
import com.coliving.api.moderation.domain.enums.CaseType
import com.coliving.api.moderation.domain.repository.SupportCaseRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Operational indicators of the pilot's support queue (RF-084): counts by
 * state, measurable average response time (from the stored instants of the
 * closed cases) and the open publication reports (RF-025). Read-only and
 * computed from the same repository the queue uses, so the numbers always
 * agree with what the moderators see.
 */
@Service
class OperationsIndicatorsService(
    private val caseRepository: SupportCaseRepository,
) {

    @Transactional(readOnly = true)
    fun indicators(): ModerationMetricsView {
        val cases = caseRepository.search(
            type = null,
            status = null,
            priority = null,
            assigneeUserId = null,
            onlyOpen = false,
        )
        val resolved = cases.filter { it.status == CaseStatus.RESUELTO }
        val resolutionHours = resolved.mapNotNull { it.resolutionHours() }
        return ModerationMetricsView(
            totalCases = cases.size,
            openCases = cases.count { it.isOpen() },
            inReviewCases = cases.count { it.status == CaseStatus.EN_REVISION },
            resolvedCases = resolved.size,
            rejectedCases = cases.count { it.status == CaseStatus.RECHAZADO },
            averageResolutionHours = resolutionHours.takeIf { it.isNotEmpty() }?.average(),
            openPublicationReports = cases.count {
                it.type == CaseType.REPORTE && it.subjectType == CaseSubjectType.PUBLICACION && it.isOpen()
            },
        )
    }
}