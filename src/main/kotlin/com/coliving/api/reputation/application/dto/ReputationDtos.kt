package com.coliving.api.reputation.application.dto

import com.coliving.api.reputation.domain.enums.EvaluationCategory
import com.coliving.api.reputation.domain.enums.EvaluationStatus
import com.coliving.api.reputation.domain.enums.RelationType
import com.coliving.api.reputation.domain.model.ScoreFactor
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Evaluation as returned to both parties (RF-070). */
data class EvaluationView(
    val id: UUID,
    val authorUserId: UUID,
    val subjectUserId: UUID,
    val relationType: RelationType,
    val relationId: UUID,
    val ratings: Map<EvaluationCategory, Int>,
    val overallRating: BigDecimal,
    val comment: String?,
    val status: EvaluationStatus,
    val disputeCaseId: UUID?,
    val createdAt: Instant,
)

/** Command to evaluate the counterpart of an eligible relation (RF-070). */
data class CreateEvaluationCommand(
    val authorUserId: UUID,
    val relationType: RelationType,
    val relationId: UUID,
    val ratings: Map<EvaluationCategory, Int>,
    val comment: String?,
)

/** Command to dispute an evaluation (RF-072). */
data class DisputeEvaluationCommand(
    val evaluationId: UUID,
    val disputantUserId: UUID,
    val reason: String,
)

/** Consultable reputation with its documented factors (RF-071). */
data class ReputationView(
    val userId: UUID,
    val score: BigDecimal,
    val formulaVersion: Int,
    val factors: List<ScoreFactor>,
    /** Benefit codes the current score grants (RF-073). */
    val benefits: List<String>,
    val activeEvaluations: Int,
    val disputedEvaluations: Int,
    val confirmedStays: Int,
    val computedAt: Instant,
)

/** Administration of the benefit rules (RF-073). */
data class BenefitRuleView(
    val id: UUID,
    val code: String,
    val description: String?,
    val minScore: BigDecimal,
    val active: Boolean,
    val createdAt: Instant,
)

/** Command to configure a benefit rule. */
data class CreateBenefitRuleCommand(
    val code: String,
    val description: String?,
    val minScore: BigDecimal,
)