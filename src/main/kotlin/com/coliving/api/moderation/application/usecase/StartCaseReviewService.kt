package com.coliving.api.moderation.application.usecase

import com.coliving.api.moderation.application.dto.CaseView
import com.coliving.api.moderation.application.dto.StartCaseReviewCommand
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
 * Moves a case into review (RF-082): support acknowledged it and started to
 * work on it. The transition is idempotent-friendly (a case already in review
 * simply stays there) but every call is recorded.
 */
@Service
class StartCaseReviewService(
    private val caseRepository: SupportCaseRepository,
    private val caseEventRepository: CaseEventRepository,
) {

    @Transactional
    fun start(command: StartCaseReviewCommand): CaseView {
        val case = caseRepository.findById(command.caseId) ?: throw NotFoundException("Case not found")
        val now = Instant.now()
        case.startReview(now)
        caseRepository.save(case)
        caseEventRepository.save(
            CaseEvent.record(
                id = UUID.randomUUID(),
                caseId = case.id,
                type = CaseEventType.EN_REVISION,
                actorUserId = command.moderatorId,
                note = "Review started",
                now = now,
            ),
        )
        return case.toView()
    }
}