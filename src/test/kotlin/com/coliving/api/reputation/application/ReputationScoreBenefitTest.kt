package com.coliving.api.reputation.application

import com.coliving.api.reputation.application.dto.CreateBenefitRuleCommand
import com.coliving.api.reputation.application.dto.CreateEvaluationCommand
import com.coliving.api.reputation.application.dto.DisputeEvaluationCommand
import com.coliving.api.reputation.application.usecase.BenefitRuleService
import com.coliving.api.reputation.application.usecase.CreateEvaluationService
import com.coliving.api.reputation.application.usecase.DisputeEvaluationService
import com.coliving.api.reputation.application.usecase.GetReputationService
import com.coliving.api.reputation.domain.enums.EvaluationCategory
import com.coliving.api.reputation.domain.enums.RelationType
import com.coliving.api.reputation.domain.model.ScoreFormula
import com.coliving.api.shared.error.ConflictException
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Score computation with its factors, and benefit rules as data (RF-071, RF-073). */
class ReputationScoreBenefitTest {

    private val relations = FakeEligibleRelationPort()
    private val verification = FakeUserVerificationPort()
    private val disputes = FakeDisputeCasePort()
    private val evaluations = FakeEvaluationRepository()
    private val scores = FakeReputationScoreRepository()
    private val benefitRules = FakeBenefitRuleRepository()
    private val createEvaluation = CreateEvaluationService(evaluations, relations)
    private val disputeEvaluation = DisputeEvaluationService(evaluations, disputes)
    private val getReputation = GetReputationService(
        evaluations, scores, benefitRules, relations, verification,
        ScoreFormula(1, BigDecimal("0.5"), BigDecimal("0.2"), BigDecimal("0.2"), BigDecimal("0.1")),
    )
    private val benefitService = BenefitRuleService(benefitRules)

    private val guestId = UUID.randomUUID()
    private val hostId = UUID.randomUUID()

    private fun perfectRatings() = mapOf(
        EvaluationCategory.LIMPIEZA to 5,
        EvaluationCategory.COMUNICACION to 5,
        EvaluationCategory.EXACTITUD to 5,
        EvaluationCategory.RESPETO to 5,
        EvaluationCategory.CUMPLIMIENTO to 5,
    )

    @Test
    fun `the score derives from evaluations, stays and verification, with benefits (RF-071, RF-073)`() {
        benefitService.create(
            CreateBenefitRuleCommand("DESCUENTO_SERVICIO", "10% de descuento", BigDecimal("4.00")),
        )
        val relation = relations.addRelation(guestId = guestId, hostId = hostId)
        relations.stayCounts[guestId] = 3
        verification.levels[guestId] = "COMPLETO"
        // The host evaluates the guest: the received evaluation drives the rating factor.
        createEvaluation.create(
            CreateEvaluationCommand(hostId, RelationType.ESTANCIA, relation.relationId, perfectRatings(), null),
        )

        val reputation = getReputation.reputationOf(guestId)

        // rating 1 x 0.5 + stays 1 x 0.2 + verification 1 x 0.2 + disputes 1 x 0.1 -> 5.00
        assertEquals(BigDecimal("5.00"), reputation.score)
        assertEquals(listOf("DESCUENTO_SERVICIO"), reputation.benefits)
        assertEquals(1, reputation.activeEvaluations)
        assertEquals(0, reputation.disputedEvaluations)
        assertEquals(3, reputation.confirmedStays)
        assertEquals(4, reputation.factors.size)
    }

    @Test
    fun `a disputed evaluation stops counting for the score (RF-072)`() {
        benefitService.create(
            CreateBenefitRuleCommand("DESCUENTO_SERVICIO", null, BigDecimal("4.00")),
        )
        val relation = relations.addRelation(guestId = guestId, hostId = hostId)
        relations.stayCounts[hostId] = 1
        val evaluation = createEvaluation.create(
            CreateEvaluationCommand(guestId, RelationType.ESTANCIA, relation.relationId, perfectRatings(), "Regular"),
        )
        disputeEvaluation.dispute(DisputeEvaluationCommand(evaluation.id, hostId, "Injusta"))

        val reputation = getReputation.reputationOf(hostId)

        assertTrue { reputation.score < BigDecimal("5.00") }
        assertEquals(0, reputation.activeEvaluations)
        assertEquals(1, reputation.disputedEvaluations)
        assertTrue { reputation.benefits.isEmpty() }
    }

    @Test
    fun `benefit rules are data with duplicated codes rejected and deactivation not deletion`() {
        val rule = benefitService.create(
            CreateBenefitRuleCommand("prioridad_cola", "Prioridad en soporte", BigDecimal("3.50")),
        )

        assertFailsWith<ConflictException> {
            benefitService.create(CreateBenefitRuleCommand("PRIORIDAD_COLA", null, BigDecimal("4.00")))
        }
        val deactivated = benefitService.deactivate(rule.id)
        assertTrue { !deactivated.active }
        assertTrue { benefitService.list(activeOnly = true).isEmpty() }
        assertEquals(1, benefitService.list(activeOnly = false).size)
        assertFailsWith<ConflictException> {
            benefitService.create(CreateBenefitRuleCommand("PRIORIDAD_COLA", null, BigDecimal("4.00")))
        }
    }
}