package com.coliving.api.reputation.domain.model

import com.coliving.api.shared.error.InvalidArgumentException
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Computed reputation of a user (RF-071). It keeps the score together with the
 * factors and the version of the formula that produced it, so the value stored
 * is always explainable and the formula can evolve without rewriting history.
 */
class ReputationScore(
    val userId: UUID,
    val score: BigDecimal,
    val formulaVersion: Int,
    val factors: List<ScoreFactor>,
    val computedAt: Instant,
)

/**
 * Configuration rule that maps a score to a benefit (RF-073: "los beneficios
 * asociados al score se administrarán mediante reglas explícitas"). Benefits are
 * data, not code, which is exactly what the requirement asks: they can be
 * configured without modifying the formula.
 */
class BenefitRule(
    val id: UUID,
    val code: String,
    val description: String?,
    val minScore: BigDecimal,
    var active: Boolean,
    val createdAt: Instant,
) {

    /** Whether this rule grants its benefit to a user with [score]. */
    fun appliesTo(score: BigDecimal): Boolean = active && score >= minScore

    /** Deactivates the rule without deleting it (historical references survive). */
    fun deactivate() {
        active = false
    }

    companion object {
        const val MAX_CODE_LENGTH = 60
        const val MAX_DESCRIPTION_LENGTH = 200

        fun create(
            id: UUID,
            code: String,
            description: String?,
            minScore: BigDecimal,
            now: Instant,
        ): BenefitRule {
            val normalizedCode = code.trim().uppercase()
            if (normalizedCode.isEmpty()) {
                throw InvalidArgumentException("The benefit code is required")
            }
            if (normalizedCode.length > MAX_CODE_LENGTH) {
                throw InvalidArgumentException("The benefit code must not exceed $MAX_CODE_LENGTH characters")
            }
            if (minScore < BigDecimal.ZERO) {
                throw InvalidArgumentException("The minimum score must not be negative")
            }
            val normalizedDescription = description?.trim()?.takeIf { it.isNotEmpty() }
            if (normalizedDescription != null && normalizedDescription.length > MAX_DESCRIPTION_LENGTH) {
                throw InvalidArgumentException(
                    "The description must not exceed $MAX_DESCRIPTION_LENGTH characters",
                )
            }
            return BenefitRule(
                id = id,
                code = normalizedCode,
                description = normalizedDescription,
                minScore = minScore,
                active = true,
                createdAt = now,
            )
        }
    }
}