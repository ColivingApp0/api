package com.coliving.api.moderation.application.usecase

import com.coliving.api.moderation.application.dto.CaseDetailView
import com.coliving.api.moderation.application.dto.CaseEventView
import com.coliving.api.moderation.application.dto.CaseView
import com.coliving.api.moderation.domain.model.CaseEvent
import com.coliving.api.moderation.domain.model.SupportCase
import com.coliving.api.moderation.domain.repository.CaseEventRepository
import com.coliving.api.moderation.domain.repository.SupportCaseRepository
import com.coliving.api.shared.error.NotFoundException
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read side of the support queue (RF-082). The projections are shared by the
 * queue listing, the case detail and the actions, so every response carries the
 * same derived fields — including the measurable response time.
 */
@Service
class GetCaseService(
    private val caseRepository: SupportCaseRepository,
    private val caseEventRepository: CaseEventRepository,
) {

    @Transactional(readOnly = true)
    fun detail(caseId: UUID): CaseDetailView {
        val case = caseRepository.findById(caseId) ?: throw NotFoundException("Case not found")
        return CaseDetailView(
            case = case.toView(),
            events = caseEventRepository.findByCase(caseId).map { it.toView() },
        )
    }

    @Transactional(readOnly = true)
    fun require(caseId: UUID): SupportCase =
        caseRepository.findById(caseId) ?: throw NotFoundException("Case not found")
}

/** Shared case projection: the response time is always derived from the instants. */
internal fun SupportCase.toView(): CaseView =
    CaseView(
        id = id,
        type = type,
        subjectType = subjectType,
        subjectId = subjectId,
        reportedUserId = reportedUserId,
        openedByUserId = openedByUserId,
        description = description,
        priority = priority,
        status = status,
        assigneeUserId = assigneeUserId,
        resolution = resolution,
        openedAt = openedAt,
        updatedAt = updatedAt,
        closedAt = closedAt,
        resolutionHours = resolutionHours(),
    )

internal fun CaseEvent.toView(): CaseEventView =
    CaseEventView(
        id = id,
        type = type,
        actorUserId = actorUserId,
        note = note,
        occurredAt = occurredAt,
    )