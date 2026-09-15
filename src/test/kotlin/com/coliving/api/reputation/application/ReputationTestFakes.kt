package com.coliving.api.reputation.application

import com.coliving.api.reputation.application.port.out.DisputeCasePort
import com.coliving.api.reputation.application.port.out.EligibleRelation
import com.coliving.api.reputation.application.port.out.EligibleRelationPort
import com.coliving.api.reputation.application.port.out.UserVerificationPort
import com.coliving.api.reputation.domain.model.Evaluation
import com.coliving.api.reputation.domain.model.BenefitRule
import com.coliving.api.reputation.domain.model.ReputationScore
import com.coliving.api.reputation.domain.repository.BenefitRuleRepository
import com.coliving.api.reputation.domain.repository.EvaluationRepository
import com.coliving.api.reputation.domain.repository.ReputationScoreRepository
import java.util.UUID

class FakeEligibleRelationPort : EligibleRelationPort {
    val relations = mutableMapOf<UUID, EligibleRelation>()
    val stayCounts = mutableMapOf<UUID, Int>()

    override fun findRelation(relationId: UUID): EligibleRelation? = relations[relationId]

    override fun confirmedStayCount(userId: UUID): Int = stayCounts[userId] ?: 0

    fun addRelation(
        relationId: UUID = UUID.randomUUID(),
        guestId: UUID,
        hostId: UUID,
        status: String = "CONFIRMADA",
    ): EligibleRelation =
        EligibleRelation(relationId, guestId, hostId, status, java.time.LocalDate.now().minusDays(10), java.time.LocalDate.now().minusDays(5))
            .also { relations[relationId] = it }
}

class FakeUserVerificationPort : UserVerificationPort {
    val levels = mutableMapOf<UUID, String>()

    override fun levelOf(userId: UUID): String = levels[userId] ?: "NO_VERIFICADO"
}

class FakeDisputeCasePort : DisputeCasePort {
    val openedCases = mutableListOf<UUID>()

    override fun openEvaluationDisputeCase(
        evaluationId: UUID,
        evaluatedUserId: UUID,
        disputantUserId: UUID,
        reason: String,
    ): UUID = UUID.randomUUID().also { openedCases.add(it) }
}

class FakeEvaluationRepository : EvaluationRepository {
    val store = mutableMapOf<UUID, Evaluation>()
    val existingPairs = mutableSetOf<Pair<UUID, UUID>>()

    override fun findById(id: UUID): Evaluation? = store[id]

    override fun findBySubject(subjectUserId: UUID): List<Evaluation> =
        store.values.filter { it.subjectUserId == subjectUserId }
            .sortedByDescending { it.createdAt }

    override fun findByAuthor(authorUserId: UUID): List<Evaluation> =
        store.values.filter { it.authorUserId == authorUserId }
            .sortedByDescending { it.createdAt }

    override fun existsByAuthorAndRelation(authorUserId: UUID, relationId: UUID): Boolean =
        (authorUserId to relationId) in existingPairs

    override fun save(evaluation: Evaluation) {
        store[evaluation.id] = evaluation
        existingPairs.add(evaluation.authorUserId to evaluation.relationId)
    }
}

class FakeReputationScoreRepository : ReputationScoreRepository {
    val store = mutableMapOf<UUID, ReputationScore>()

    override fun findByUser(userId: UUID): ReputationScore? = store[userId]

    override fun save(score: ReputationScore) {
        store[score.userId] = score
    }
}

class FakeBenefitRuleRepository : BenefitRuleRepository {
    val store = mutableMapOf<UUID, BenefitRule>()

    override fun findById(id: UUID): BenefitRule? = store[id]

    override fun findByCode(code: String): BenefitRule? =
        store.values.firstOrNull { it.code == code.trim().uppercase() }

    override fun findAll(activeOnly: Boolean): List<BenefitRule> =
        store.values.filter { !activeOnly || it.active }.sortedBy { it.minScore }

    override fun save(rule: BenefitRule) {
        store[rule.id] = rule
    }
}