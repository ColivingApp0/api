package com.coliving.api.reputation.application.usecase

import com.coliving.api.reputation.application.dto.EvaluationView
import com.coliving.api.reputation.domain.model.Evaluation
import com.coliving.api.reputation.domain.repository.EvaluationRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read side of the evaluations (RF-070): a user sees the evaluations they
 * received and the ones they wrote. Projections are shared with the services
 * that mutate, so every response has the same shape.
 */
@Service
class ListEvaluationsService(
    private val evaluationRepository: EvaluationRepository,
) {

    @Transactional(readOnly = true)
    fun receivedBy(userId: UUID): List<EvaluationView> =
        evaluationRepository.findBySubject(userId).map { it.toView() }

    @Transactional(readOnly = true)
    fun writtenBy(userId: UUID): List<EvaluationView> =
        evaluationRepository.findByAuthor(userId).map { it.toView() }
}

/** Shared projection of an evaluation. */
internal fun Evaluation.toView(): EvaluationView =
    EvaluationView(
        id = id,
        authorUserId = authorUserId,
        subjectUserId = subjectUserId,
        relationType = relationType,
        relationId = relationId,
        ratings = ratings,
        overallRating = overallRating(),
        comment = comment,
        status = status,
        disputeCaseId = disputeCaseId,
        createdAt = createdAt,
    )