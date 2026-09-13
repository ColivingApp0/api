package com.coliving.api.moderation.application.usecase

import com.coliving.api.moderation.application.dto.AssignCaseCommand
import com.coliving.api.moderation.application.dto.CaseView
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
 * Assigns a case to a moderator (RF-082: every case has a responsible person).
 * Reassignment is allowed while the case is open, and each change is part of the
 * traceability chain.
 */
@Service
class AssignCaseService(
    private val caseRepository: SupportCaseRepository,
    private val caseEventRepository: CaseEventRepository,
) {

    @Transactional
    fun assign(command: AssignCaseCommand): CaseView {
        val case = caseRepository.findById(command.caseId) ?: throw NotFoundException("Case not found")
        val now = Instant.now()
        case.assign(command.moderatorId, now)
        caseRepository.save(case)
        caseEventRepository.save(
            CaseEvent.record(
                id = UUID.randomUUID(),
                caseId = case.id,
                type = CaseEventType.ASIGNADO,
                actorUserId = command.moderatorId,
                note = "Assigned to moderator",
                now = now,
            ),
        )
        return case.toView()
    }
}