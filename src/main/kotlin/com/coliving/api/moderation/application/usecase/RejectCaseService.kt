package com.coliving.api.moderation.application.usecase

import com.coliving.api.moderation.application.dto.CaseView
import com.coliving.api.moderation.application.dto.RejectCaseCommand
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
 * Rejects a case (no action taken). The reason is mandatory (RF-082) and the
 * affected user can always consult why nothing changed.
 */
@Service
class RejectCaseService(
    private val caseRepository: SupportCaseRepository,
    private val caseEventRepository: CaseEventRepository,
) {

    @Transactional
    fun reject(command: RejectCaseCommand): CaseView {
        val case = caseRepository.findById(command.caseId) ?: throw NotFoundException("Case not found")
        val now = Instant.now()
        case.reject(command.reason, now)
        caseRepository.save(case)
        caseEventRepository.save(
            CaseEvent.record(
                id = UUID.randomUUID(),
                caseId = case.id,
                type = CaseEventType.RECHAZADO,
                actorUserId = command.moderatorId,
                note = case.resolution,
                now = now,
            ),
        )
        return case.toView()
    }
}