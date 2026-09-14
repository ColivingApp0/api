package com.coliving.api.reputation.domain.repository

import com.coliving.api.reputation.domain.model.BenefitRule
import com.coliving.api.reputation.domain.model.Evaluation
import com.coliving.api.reputation.domain.model.ReputationScore
import java.util.UUID

interface EvaluationRepository {

    fun findById(id: UUID): Evaluation?

    /** Evaluations received by a user, newest first. */
    fun findBySubject(subjectUserId: UUID): List<Evaluation>

    /** Evaluations written by a user, newest first. */
    fun findByAuthor(authorUserId: UUID): List<Evaluation>

    /** Uniqueness of the evaluation per event: one evaluation per author and relation (RF-070). */
    fun existsByAuthorAndRelation(authorUserId: UUID, relationId: UUID): Boolean

    fun save(evaluation: Evaluation)
}

interface ReputationScoreRepository {

    /** Last computed score of a user, or null when it was never computed. */
    fun findByUser(userId: UUID): ReputationScore?

    /** Persists the computed score, replacing the previous snapshot. */
    fun save(score: ReputationScore)
}

interface BenefitRuleRepository {

    fun findById(id: UUID): BenefitRule?

    fun findByCode(code: String): BenefitRule?

    /** Rules ordered by minimum score; only active ones when [activeOnly]. */
    fun findAll(activeOnly: Boolean): List<BenefitRule>

    fun save(rule: BenefitRule)
}