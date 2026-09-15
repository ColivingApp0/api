package com.coliving.api.reputation.application.usecase

import com.coliving.api.reputation.application.dto.CreateEvaluationCommand
import com.coliving.api.reputation.application.dto.EvaluationView
import com.coliving.api.reputation.application.port.out.EligibleRelationPort
import com.coliving.api.reputation.domain.model.Evaluation
import com.coliving.api.reputation.domain.repository.EvaluationRepository
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates an evaluation between the two parties of an eligible relation
 * (RF-070). Two gates of the requirement are enforced here:
 *
 * - **only users linked by a valid relation**: the relation is resolved through
 *   booking and must be a CONFIRMADA stay, and the author must be one of its
 *   parties (the subject is always the counterpart, never chosen by the client);
 * - **a single evaluation per event**: one author can evaluate a given relation
 *   once.
 */
@Service
class CreateEvaluationService(
    private val evaluationRepository: EvaluationRepository,
    private val eligibleRelationPort: EligibleRelationPort,
) {

    @Transactional
    fun create(command: CreateEvaluationCommand): EvaluationView {
        val relation = eligibleRelationPort.findRelation(command.relationId)
            ?: throw NotFoundException("The relation to evaluate does not exist")
        if (relation.status != ELIGIBLE_STATUS) {
            throw ConflictException("Only a confirmed stay can be evaluated")
        }
        if (command.authorUserId !in relation.parties()) {
            throw ForbiddenException("Only the parties of the relation can evaluate it")
        }
        if (evaluationRepository.existsByAuthorAndRelation(command.authorUserId, command.relationId)) {
            throw ConflictException("This relation was already evaluated")
        }

        val subjectUserId = relation.counterpartOf(command.authorUserId)
        val evaluation = Evaluation.create(
            id = UUID.randomUUID(),
            authorUserId = command.authorUserId,
            subjectUserId = subjectUserId,
            relationType = command.relationType,
            relationId = command.relationId,
            ratings = command.ratings,
            comment = command.comment,
            now = Instant.now(),
        )
        evaluationRepository.save(evaluation)
        return evaluation.toView()
    }

    companion object {
        /** Reservation status that makes a relation eligible (RF-070). */
        const val ELIGIBLE_STATUS = "CONFIRMADA"
    }
}