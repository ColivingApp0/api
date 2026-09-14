package com.coliving.api.reputation.application

import com.coliving.api.reputation.application.dto.CreateEvaluationCommand
import com.coliving.api.reputation.application.dto.DisputeEvaluationCommand
import com.coliving.api.reputation.application.usecase.CreateEvaluationService
import com.coliving.api.reputation.application.usecase.DisputeEvaluationService
import com.coliving.api.reputation.domain.enums.EvaluationCategory
import com.coliving.api.reputation.domain.enums.EvaluationStatus
import com.coliving.api.reputation.domain.enums.RelationType
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Evaluation gates and dispute flow (RF-070, RF-072). */
class ReputationEvaluationTest {

    private val relations = FakeEligibleRelationPort()
    private val disputes = FakeDisputeCasePort()
    private val evaluations = FakeEvaluationRepository()
    private val createEvaluation = CreateEvaluationService(evaluations, relations)
    private val disputeEvaluation = DisputeEvaluationService(evaluations, disputes)

    private val guestId = UUID.randomUUID()
    private val hostId = UUID.randomUUID()

    private fun fullRatings() = mapOf(
        EvaluationCategory.LIMPIEZA to 4,
        EvaluationCategory.COMUNICACION to 4,
        EvaluationCategory.EXACTITUD to 4,
        EvaluationCategory.RESPETO to 4,
        EvaluationCategory.CUMPLIMIENTO to 4,
    )

    @Test
    fun `guest and host evaluate each other once per confirmed stay (RF-070)`() {
        val relation = relations.addRelation(guestId = guestId, hostId = hostId)

        val evaluation = createEvaluation.create(
            CreateEvaluationCommand(
                authorUserId = guestId,
                relationType = RelationType.ESTANCIA,
                relationId = relation.relationId,
                ratings = fullRatings(),
                comment = "Anfitrión excelente",
            ),
        )

        assertEquals(hostId, evaluation.subjectUserId)
        assertEquals(EvaluationStatus.ACTIVA, evaluation.status)
        assertFailsWith<ConflictException> {
            createEvaluation.create(
                CreateEvaluationCommand(
                    guestId, RelationType.ESTANCIA, relation.relationId, fullRatings(), null,
                ),
            )
        }
    }

    @Test
    fun `only parties of a confirmed stay can evaluate it`() {
        val relation = relations.addRelation(guestId = guestId, hostId = hostId)

        assertFailsWith<ForbiddenException> {
            createEvaluation.create(
                CreateEvaluationCommand(
                    UUID.randomUUID(), RelationType.ESTANCIA, relation.relationId, fullRatings(), null,
                ),
            )
        }
    }

    @Test
    fun `a non confirmed relation is not eligible (RF-070)`() {
        val relation = relations.addRelation(
            guestId = guestId, hostId = hostId, status = "SOLICITADA",
        )

        assertFailsWith<ConflictException> {
            createEvaluation.create(
                CreateEvaluationCommand(
                    guestId, RelationType.ESTANCIA, relation.relationId, fullRatings(), null,
                ),
            )
        }
    }

    @Test
    fun `disputing suspends the evaluation through a moderation case (RF-072)`() {
        val relation = relations.addRelation(guestId = guestId, hostId = hostId)
        val evaluation = createEvaluation.create(
            CreateEvaluationCommand(guestId, RelationType.ESTANCIA, relation.relationId, fullRatings(), "Regular"),
        )

        val disputed = disputeEvaluation.dispute(
            DisputeEvaluationCommand(evaluation.id, hostId, "No fue justa"),
        )

        assertEquals(EvaluationStatus.DISPUTADA, disputed.status)
        assertEquals(1, disputes.openedCases.size)
        assertEquals(disputes.openedCases.single(), disputed.disputeCaseId)
        assertFailsWith<ForbiddenException> {
            disputeEvaluation.dispute(DisputeEvaluationCommand(evaluation.id, guestId, "Soy el autor"))
        }
        assertFailsWith<ConflictException> {
            disputeEvaluation.dispute(DisputeEvaluationCommand(evaluation.id, hostId, "Otra vez"))
        }
    }
}