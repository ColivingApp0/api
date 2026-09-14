package com.coliving.api.reputation.domain

import com.coliving.api.reputation.domain.model.ScoreFormula
import com.coliving.api.reputation.domain.model.ScoreInputs
import com.coliving.api.shared.error.InvalidArgumentException
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The documented formula of RF-071: factors with configurable weights, a 0..5
 * scale, and a dispute penalty that never vetoes the score.
 */
class ScoreFormulaTest {

    private fun formula(
        version: Int = 1,
        rating: String = "0.5",
        stays: String = "0.2",
        verification: String = "0.2",
        disputes: String = "0.1",
    ) = ScoreFormula(
        version = version,
        ratingWeight = BigDecimal(rating),
        staysWeight = BigDecimal(stays),
        verificationWeight = BigDecimal(verification),
        disputesWeight = BigDecimal(disputes),
    )

    private fun inputs(
        averageRating: BigDecimal? = BigDecimal("4.50"),
        confirmedStays: Int = 2,
        verificationLevel: String = "COMPLETO",
        disputed: Int = 0,
    ) = ScoreInputs(averageRating, confirmedStays, verificationLevel, disputed)

    @Test
    fun `weights must be non negative and add up to 1`() {
        formula()
        assertFailsWith<InvalidArgumentException> { formula(rating = "0.6") }
        assertFailsWith<InvalidArgumentException> { formula(rating = "-0.5", stays = "0.7") }
        assertFailsWith<InvalidArgumentException> { formula(version = 0) }
    }

    @Test
    fun `the score is on a 0 to 5 scale with the four documented factors`() {
        val computation = formula().evaluate(inputs())

        assertEquals(BigDecimal("4.42"), computation.score)
        assertEquals(1, computation.formulaVersion)
        assertEquals(
            listOf(ScoreFormula.RATING, ScoreFormula.STAYS, ScoreFormula.VERIFICATION, ScoreFormula.DISPUTES),
            computation.factors.map { it.name },
        )
        // rating 0.9 x 0.5 = 0.4500; stays 0.6667 x 0.2 = 0.1333
        assertEquals(BigDecimal("0.4500"), computation.factors[0].contribution)
        assertEquals(BigDecimal("0.1333"), computation.factors[1].contribution)
    }

    @Test
    fun `a user without evaluations is not penalized below the floor of the other factors`() {
        val computation = formula().evaluate(
            inputs(averageRating = null, confirmedStays = 0, verificationLevel = "NO_VERIFICADO"),
        )

        // Only the disputes factor (1.0 x 0.1) contributes: 0.5
        assertEquals(BigDecimal("0.50"), computation.score)
    }

    @Test
    fun `stays saturate and verification levels map to their normalized value`() {
        val saturated = formula().evaluate(
            inputs(averageRating = BigDecimal("5"), confirmedStays = 10, verificationLevel = "COMPLETO"),
        )
        assertEquals(BigDecimal("5.00"), saturated.score)

        val basic = formula().evaluate(
            inputs(averageRating = null, confirmedStays = 0, verificationLevel = "BASICO"),
        )
        // verification 0.5 x 0.2 = 0.1 + disputes 1 x 0.1 = 0.1 -> 0.2 x 5
        assertEquals(BigDecimal("1.00"), basic.score)
    }

    @Test
    fun `disputed evaluations reduce the score but never veto it`() {
        val withoutDisputes = formula().evaluate(inputs(disputed = 0))
        val withDisputes = formula().evaluate(inputs(disputed = 1))

        assertTrue { withDisputes.score < withoutDisputes.score }
        assertTrue { withDisputes.score > BigDecimal.ZERO }
        assertEquals(
            BigDecimal("0.5000"),
            withDisputes.factors.last().normalizedValue,
        )
    }
}