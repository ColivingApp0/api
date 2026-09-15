package com.coliving.api.moderation.application.usecase

import com.coliving.api.moderation.application.dto.CaseView
import com.coliving.api.moderation.application.dto.ResolveCaseCommand
import com.coliving.api.moderation.domain.enums.CaseEventType
import com.coliving.api.moderation.domain.model.CaseEvent
import com.coliving.api.moderation.domain.repository.CaseEventRepository
import com.coliving.api.moderation.domain.repository.SupportCaseRepository
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Resolves a case (RF-082). The mandatory resolution is what makes the decision
 * auditable and, together with the stored instants, what makes the response
 * time measurable.
 */
@Service
class ResolveCaseService(
    private val caseRepository: SupportCaseRepository,
    private val caseEventRepository: CaseEventRepository,
) {

    @Transactional
    fun resolve(command: ResolveCaseCommand): CaseView {
        val case = caseRepository.findById(command.caseId) ?: throw NotFoundException("Case not found")
        val now = Instant.now()
        case.resolve(command.resolution, now)
        caseRepository.save(case)
        caseEventRepository.save(
            CaseEvent.record(
                id = UUID.randomUUID(),
                caseId = case.id,
                type = CaseEventType.RESUELTO,
                actorUserId = command.moderatorId,
                note = case.resolution,
                now = now,
            ),
        )
        return case.toView()
    }
}