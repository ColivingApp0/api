package com.coliving.api.reputation.presentation.dto

import com.coliving.api.reputation.domain.enums.EvaluationCategory
import com.coliving.api.reputation.domain.enums.RelationType
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.UUID

/** Creates an evaluation over an eligible relation (RF-070). */
data class CreateEvaluationRequest(
    @field:NotNull
    val relationType: RelationType,
    @field:NotNull
    val relationId: UUID,
    /** Every defined category must be rated 1..5. */
    val ratings: Map<EvaluationCategory, @Min(1) @Max(5) Int>,
    @field:Size(max = 1000)
    val comment: String? = null,
)

/** Disputes an evaluation; the reason is the description of the case (RF-072). */
data class DisputeEvaluationRequest(
    @field:NotBlank
    @field:Size(max = 1000)
    val reason: String,
)

/** Configures a benefit rule over the score (RF-073). */
data class CreateBenefitRuleRequest(
    @field:NotBlank
    @field:Size(max = 60)
    val code: String,
    @field:Size(max = 200)
    val description: String? = null,
    @field:NotNull
    @field:Min(0)
    @field:Max(5)
    val minScore: BigDecimal,
)