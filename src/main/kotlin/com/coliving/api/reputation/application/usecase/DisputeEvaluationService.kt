package com.coliving.api.reputation.application.usecase

import com.coliving.api.reputation.application.dto.DisputeEvaluationCommand
import com.coliving.api.reputation.application.dto.EvaluationView
import com.coliving.api.reputation.application.port.out.DisputeCasePort
import com.coliving.api.reputation.domain.repository.EvaluationRepository
import com.coliving.api.shared.error.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Disputes an evaluation (RF-072). The dispute opens a moderation case — the
 * same auditable queue the support team already uses — and suspends the effect
 * of the evaluation on the score until the case is resolved, which is the
 * behavior the requirement asks for.
 */
@Service
class DisputeEvaluationService(
    private val evaluationRepository: EvaluationRepository,
    private val disputeCasePort: DisputeCasePort,
) {

    @Transactional
    fun dispute(command: DisputeEvaluationCommand): EvaluationView {
        val evaluation = evaluationRepository.findById(command.evaluationId)
            ?: throw NotFoundException("Evaluation not found")
        // Only the evaluated user can dispute (checked by the aggregate).
        evaluation.requireSubject(command.disputantUserId)

        val caseId = disputeCasePort.openEvaluationDisputeCase(
            evaluationId = evaluation.id,
            evaluatedUserId = evaluation.subjectUserId,
            disputantUserId = command.disputantUserId,
            reason = command.reason,
        )
        evaluation.dispute(caseId)
        evaluationRepository.save(evaluation)
        return evaluation.toView()
    }
}