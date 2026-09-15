package com.coliving.api.reputation.domain

import com.coliving.api.reputation.domain.enums.EvaluationCategory
import com.coliving.api.reputation.domain.enums.EvaluationStatus
import com.coliving.api.reputation.domain.enums.RelationType
import com.coliving.api.reputation.domain.model.Evaluation
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.InvalidArgumentException
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Invariants of the evaluation aggregate (RF-070, RF-072). */
class EvaluationTest {

    private val authorId = UUID.randomUUID()
    private val subjectId = UUID.randomUUID()
    private val now = Instant.parse("2026-09-13T15:00:00Z")

    private fun ratings() = mapOf(
        EvaluationCategory.LIMPIEZA to 5,
        EvaluationCategory.COMUNICACION to 4,
        EvaluationCategory.EXACTITUD to 5,
        EvaluationCategory.RESPETO to 4,
        EvaluationCategory.CUMPLIMIENTO to 5,
    )

    private fun evaluation(): Evaluation = Evaluation.create(
        id = UUID.randomUUID(),
        authorUserId = authorId,
        subjectUserId = subjectId,
        relationType = RelationType.ESTANCIA,
        relationId = UUID.randomUUID(),
        ratings = ratings(),
        comment = "  Excelente huésped  ",
        now = now,
    )

    @Test
    fun `creates an active evaluation with every category rated`() {
        val evaluation = evaluation()

        assertEquals(EvaluationStatus.ACTIVA, evaluation.status)
        assertTrue { evaluation.countsForScore() }
        assertEquals("Excelente huésped", evaluation.comment)
        assertEquals(BigDecimal("4.60"), evaluation.overallRating())
    }

    @Test
    fun `nobody evaluates themselves`() {
        assertFailsWith<InvalidArgumentException> {
            Evaluation.create(
                UUID.randomUUID(), authorId, authorId, RelationType.ESTANCIA,
                UUID.randomUUID(), ratings(), null, now,
            )
        }
    }

    @Test
    fun `every defined category is mandatory and ratings stay within 1 - 5`() {
        val incomplete = ratings().filterKeys { it != EvaluationCategory.RESPETO }
        assertFailsWith<InvalidArgumentException> {
            Evaluation.create(
                UUID.randomUUID(), authorId, subjectId, RelationType.ESTANCIA,
                UUID.randomUUID(), incomplete, null, now,
            )
        }
        val outOfRange = ratings().plus(EvaluationCategory.RESPETO to 6)
        assertFailsWith<InvalidArgumentException> {
            Evaluation.create(
                UUID.randomUUID(), authorId, subjectId, RelationType.ESTANCIA,
                UUID.randomUUID(), outOfRange, null, now,
            )
        }
        val belowRange = ratings().plus(EvaluationCategory.RESPETO to 0)
        assertFailsWith<InvalidArgumentException> {
            Evaluation.create(
                UUID.randomUUID(), authorId, subjectId, RelationType.ESTANCIA,
                UUID.randomUUID(), belowRange, null, now,
            )
        }
    }

    @Test
    fun `only the evaluated user can dispute it (RF-072)`() {
        val evaluation = evaluation()

        assertFailsWith<ForbiddenException> { evaluation.requireSubject(authorId) }
        evaluation.requireSubject(subjectId)
    }

    @Test
    fun `disputing suspends the score effect and links the moderation case`() {
        val evaluation = evaluation()
        val caseId = UUID.randomUUID()

        evaluation.dispute(caseId)

        assertEquals(EvaluationStatus.DISPUTADA, evaluation.status)
        assertEquals(caseId, evaluation.disputeCaseId)
        assertTrue { !evaluation.countsForScore() }
        assertFailsWith<ConflictException> { evaluation.dispute(caseId) }
    }
}