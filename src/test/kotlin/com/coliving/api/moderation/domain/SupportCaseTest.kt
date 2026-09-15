package com.coliving.api.moderation.domain

import com.coliving.api.moderation.domain.enums.CasePriority
import com.coliving.api.moderation.domain.enums.CaseStatus
import com.coliving.api.moderation.domain.enums.CaseSubjectType
import com.coliving.api.moderation.domain.enums.CaseType
import com.coliving.api.moderation.domain.model.CaseEvent
import com.coliving.api.moderation.domain.model.SupportCase
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.InvalidArgumentException
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Invariants of a support case (RF-082) and its traceability entries. */
class SupportCaseTest {

    private val reporterId = UUID.randomUUID()
    private val reportedId = UUID.randomUUID()
    private val moderatorId = UUID.randomUUID()
    private val openedAt = Instant.parse("2026-09-13T10:00:00Z")

    private fun conversationCase(): SupportCase = SupportCase.open(
        id = UUID.randomUUID(),
        type = CaseType.REPORTE,
        subjectType = CaseSubjectType.CONVERSACION,
        subjectId = UUID.randomUUID(),
        reportedUserId = reportedId,
        openedByUserId = reporterId,
        description = "Mensajes abusivos en la conversación",
        priority = CasePriority.ALTA,
        now = openedAt,
    )

    @Test
    fun `opens a report with description, priority and open state`() {
        val case = conversationCase()

        assertEquals(CaseStatus.ABIERTO, case.status)
        assertEquals(CasePriority.ALTA, case.priority)
        assertTrue { case.isOpen() }
        assertNull(case.closedAt)
        assertNull(case.resolutionHours())
    }

    @Test
    fun `rejects an empty description and overlong notes`() {
        assertFailsWith<InvalidArgumentException> {
            SupportCase.open(
                UUID.randomUUID(), CaseType.REPORTE, CaseSubjectType.USUARIO,
                UUID.randomUUID(), reportedId, reporterId, "   ",
                CasePriority.BAJA, openedAt,
            )
        }
        assertFailsWith<InvalidArgumentException> {
            SupportCase.open(
                UUID.randomUUID(), CaseType.REPORTE, CaseSubjectType.USUARIO,
                UUID.randomUUID(), reportedId, reporterId, "x".repeat(1001),
                CasePriority.BAJA, openedAt,
            )
        }
    }

    @Test
    fun `a user cannot report or dispute against themselves`() {
        assertFailsWith<InvalidArgumentException> {
            SupportCase.open(
                UUID.randomUUID(), CaseType.DISPUTA, CaseSubjectType.EVALUACION,
                UUID.randomUUID(), reporterId, reporterId,
                "Disputa propia", CasePriority.MEDIA, openedAt,
            )
        }
    }

    @Test
    fun `assignment and review only happen while the case is open`() {
        val case = conversationCase()

        case.assign(moderatorId, openedAt.plusSeconds(600))
        assertEquals(moderatorId, case.assigneeUserId)

        case.startReview(openedAt.plusSeconds(1200))
        assertEquals(CaseStatus.EN_REVISION, case.status)
    }

    @Test
    fun `resolution requires a note and closes the case`() {
        val case = conversationCase()
        val closedAt = openedAt.plusSeconds(7200)

        case.resolve("  Se sancionó al usuario reportado  ", closedAt)

        assertEquals(CaseStatus.RESUELTO, case.status)
        assertEquals("Se sancionó al usuario reportado", case.resolution)
        assertEquals(closedAt, case.closedAt)
        assertEquals(2.0, case.resolutionHours())
    }

    @Test
    fun `rejection also requires a reason`() {
        val case = conversationCase()

        case.reject("Sin evidencia suficiente", openedAt.plusSeconds(1800))

        assertEquals(CaseStatus.RECHAZADO, case.status)
        assertEquals("Sin evidencia suficiente", case.resolution)
    }

    @Test
    fun `terminal states are final`() {
        val case = conversationCase()
        case.resolve("Cerrado", openedAt.plusSeconds(600))

        assertFailsWith<ConflictException> { case.resolve("Otra vez", openedAt.plusSeconds(700)) }
        assertFailsWith<ConflictException> { case.reject("Reabrir", openedAt.plusSeconds(700)) }
        assertFailsWith<ConflictException> { case.assign(moderatorId, openedAt.plusSeconds(700)) }
        assertFailsWith<ConflictException> { case.startReview(openedAt.plusSeconds(700)) }
    }

    @Test
    fun `events are recorded with actor, note and instant`() {
        val caseId = UUID.randomUUID()
        val event = CaseEvent.record(
            id = UUID.randomUUID(),
            caseId = caseId,
            type = com.coliving.api.moderation.domain.enums.CaseEventType.ASIGNADO,
            actorUserId = moderatorId,
            note = "  Asignado al moderador de turno  ",
            now = openedAt,
        )

        assertEquals(caseId, event.caseId)
        assertEquals("Asignado al moderador de turno", event.note)
        assertFailsWith<InvalidArgumentException> {
            CaseEvent.record(
                UUID.randomUUID(), caseId,
                com.coliving.api.moderation.domain.enums.CaseEventType.ASIGNADO,
                moderatorId, "x".repeat(1001), openedAt,
            )
        }
    }
}