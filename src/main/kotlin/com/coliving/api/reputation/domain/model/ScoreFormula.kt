package com.coliving.api.reputation.domain.model

import com.coliving.api.shared.error.InvalidArgumentException
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * One documented factor of the score (RF-071): its weight, the normalized value
 * it contributed and the resulting contribution. Returning the factors is what
 * lets the user "consultar los factores que influyen".
 */
data class ScoreFactor(
    val name: String,
    val weight: BigDecimal,
    val normalizedValue: BigDecimal,
    val contribution: BigDecimal,
)

/** Inputs of the formula, already resolved by the application services. */
data class ScoreInputs(
    /** Average rating of the active evaluations received, 1..5; null when none. */
    val averageRating: BigDecimal?,
    /** Confirmed stays as guest or host. */
    val confirmedStays: Int,
    /** identity's VerificationLevel name (NO_VERIFICADO / BASICO / COMPLETO). */
    val verificationLevel: String,
    /** Evaluations of this user currently under dispute (do not count). */
    val disputedEvaluations: Int,
)

/** Score plus the factors that produced it and the version of the formula. */
data class ScoreComputation(
    val score: BigDecimal,
    val formulaVersion: Int,
    val factors: List<ScoreFactor>,
)

/**
 * Reputation formula (RF-071): documented factors with configurable weights and
 * an explicit version, so a score can always be explained and audited.
 *
 * - **rating**: average of the active evaluations, normalized to 0..1.
 * - **stays**: confirmed stays, saturating at [STAYS_SATURATION].
 * - **verification**: identity's level (COMPLETO 1, BASICO 0.5, none 0).
 * - **disputes**: 1 / (1 + disputed evaluations) — a penalty, never a veto.
 *
 * The result is expressed on a 0..5 scale and rounded to two decimals. Weights
 * must add up to 1, which keeps the score comparable between configurations.
 */
data class ScoreFormula(
    val version: Int,
    val ratingWeight: BigDecimal,
    val staysWeight: BigDecimal,
    val verificationWeight: BigDecimal,
    val disputesWeight: BigDecimal,
) {

    init {
        if (version < 1) {
            throw InvalidArgumentException("The formula version must be positive")
        }
        val weights = listOf(ratingWeight, staysWeight, verificationWeight, disputesWeight)
        if (weights.any { it < BigDecimal.ZERO }) {
            throw InvalidArgumentException("Weights must not be negative")
        }
        if (weights.fold(BigDecimal.ZERO) { acc, w -> acc + w }.compareTo(BigDecimal.ONE) != 0) {
            throw InvalidArgumentException("Weights must add up to 1")
        }
    }

    fun evaluate(inputs: ScoreInputs): ScoreComputation {
        val rating = inputs.averageRating
            ?.divide(MAX_SCALE, 4, RoundingMode.HALF_UP)
            ?: BigDecimal.ZERO
        val stays = BigDecimal(minOf(inputs.confirmedStays, STAYS_SATURATION))
            .divide(BigDecimal(STAYS_SATURATION), 4, RoundingMode.HALF_UP)
        val verification = when (inputs.verificationLevel) {
            "COMPLETO" -> BigDecimal("1")
            "BASICO" -> BigDecimal("0.5")
            else -> BigDecimal.ZERO
        }
        val disputes = BigDecimal.ONE.divide(
            BigDecimal.ONE + BigDecimal(inputs.disputedEvaluations.coerceAtLeast(0)),
            4,
            RoundingMode.HALF_UP,
        )

        val factors = listOf(
            factor(RATING, ratingWeight, rating),
            factor(STAYS, staysWeight, stays),
            factor(VERIFICATION, verificationWeight, verification),
            factor(DISPUTES, disputesWeight, disputes),
        )
        val weighted = factors.fold(BigDecimal.ZERO) { acc, f -> acc + f.contribution }
        val score = (weighted * MAX_SCALE).setScale(2, RoundingMode.HALF_UP)
        return ScoreComputation(score = score, formulaVersion = version, factors = factors)
    }

    private fun factor(name: String, weight: BigDecimal, value: BigDecimal): ScoreFactor {
        val normalized = value.setScale(4, RoundingMode.HALF_UP)
        return ScoreFactor(
            name = name,
            weight = weight,
            normalizedValue = normalized,
            contribution = (weight * normalized).setScale(4, RoundingMode.HALF_UP),
        )
    }

    companion object {
        const val RATING = "rating"
        const val STAYS = "stays"
        const val VERIFICATION = "verification"
        const val DISPUTES = "disputes"

        /** Stays needed to reach the top of the stays factor. */
        const val STAYS_SATURATION = 3

        private val MAX_SCALE = BigDecimal("5")
    }
}