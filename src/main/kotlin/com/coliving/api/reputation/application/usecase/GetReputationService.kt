package com.coliving.api.reputation.application.usecase

import com.coliving.api.reputation.application.dto.ReputationView
import com.coliving.api.reputation.application.port.out.EligibleRelationPort
import com.coliving.api.reputation.application.port.out.UserVerificationPort
import com.coliving.api.reputation.domain.model.Evaluation
import com.coliving.api.reputation.domain.model.ReputationScore
import com.coliving.api.reputation.domain.model.ScoreFormula
import com.coliving.api.reputation.domain.model.ScoreInputs
import com.coliving.api.reputation.domain.repository.BenefitRuleRepository
import com.coliving.api.reputation.domain.repository.EvaluationRepository
import com.coliving.api.reputation.domain.repository.ReputationScoreRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Computes and returns the reputation of a user (RF-071).
 *
 * The score is derived from the active evaluations received, the confirmed stays
 * (booking), the verification level (identity) and the evaluations currently
 * under dispute; the factors, the weights and the version of the formula are
 * returned so the user can see what influences the result. Every computation
 * stores a snapshot, which keeps the version of the formula used auditable.
 */
@Service
class GetReputationService(
    private val evaluationRepository: EvaluationRepository,
    private val scoreRepository: ReputationScoreRepository,
    private val benefitRuleRepository: BenefitRuleRepository,
    private val eligibleRelationPort: EligibleRelationPort,
    private val userVerificationPort: UserVerificationPort,
    private val formula: ScoreFormula,
) {

    @Transactional
    fun reputationOf(userId: UUID): ReputationView {
        // Disputed evaluations suspended their effect: only the active ones count.
        val received = evaluationRepository.findBySubject(userId)
        val active = received.filter { it.countsForScore() }
        val disputed = received.size - active.size

        val score = compute(userId, active, disputed)
        scoreRepository.save(score)

        return ReputationView(
            userId = userId,
            score = score.score,
            formulaVersion = score.formulaVersion,
            factors = score.factors,
            benefits = benefitRuleRepository.findAll(activeOnly = true)
                .filter { it.appliesTo(score.score) }
                .map { it.code },
            activeEvaluations = active.size,
            disputedEvaluations = disputed,
            confirmedStays = eligibleRelationPort.confirmedStayCount(userId),
            computedAt = score.computedAt,
        )
    }

    private fun compute(
        userId: UUID,
        activeEvaluations: List<Evaluation>,
        disputedEvaluations: Int,
    ): ReputationScore {
        val averageRating: BigDecimal? = if (activeEvaluations.isEmpty()) {
            null
        } else {
            val sum = activeEvaluations.fold(BigDecimal.ZERO) { acc, e -> acc + e.overallRating() }
            sum.divide(BigDecimal(activeEvaluations.size), 2, RoundingMode.HALF_UP)
        }
        val computation = formula.evaluate(
            ScoreInputs(
                averageRating = averageRating,
                confirmedStays = eligibleRelationPort.confirmedStayCount(userId),
                verificationLevel = userVerificationPort.levelOf(userId),
                disputedEvaluations = disputedEvaluations,
            ),
        )
        return ReputationScore(
            userId = userId,
            score = computation.score,
            formulaVersion = computation.formulaVersion,
            factors = computation.factors,
            computedAt = Instant.now(),
        )
    }
}