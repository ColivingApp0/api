package com.coliving.api.moderation.application

import com.coliving.api.moderation.application.dto.AssignCaseCommand
import com.coliving.api.moderation.application.dto.CaseSearchQuery
import com.coliving.api.moderation.application.dto.RejectCaseCommand
import com.coliving.api.moderation.application.dto.ResolveCaseCommand
import com.coliving.api.moderation.application.dto.StartCaseReviewCommand
import com.coliving.api.moderation.application.usecase.AssignCaseService
import com.coliving.api.moderation.application.usecase.CaseIntakeService
import com.coliving.api.moderation.application.usecase.GetCaseService
import com.coliving.api.moderation.application.usecase.ListCasesService
import com.coliving.api.moderation.application.usecase.RejectCaseService
import com.coliving.api.moderation.application.usecase.ResolveCaseService
import com.coliving.api.moderation.application.usecase.StartCaseReviewService
import com.coliving.api.moderation.domain.enums.CaseEventType
import com.coliving.api.moderation.domain.enums.CasePriority
import com.coliving.api.moderation.domain.enums.CaseStatus
import com.coliving.api.moderation.domain.enums.CaseSubjectType
import com.coliving.api.moderation.domain.enums.CaseType
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.InvalidArgumentException
import com.coliving.api.shared.error.NotFoundException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The support queue of RF-082 exercised end to end: a report raised through the
 * intake (the only door `messaging`/`reputation` use), the moderator workflow
 * and the mandatory, auditable notes.
 */
class CaseLifecycleTest {

    private val cases = FakeSupportCaseRepository()
    private val events = FakeCaseEventRepository()
    private val intake = CaseIntakeService(cases, events)
    private val getCase = GetCaseService(cases, events)
    private val listCases = ListCasesService(cases)
    private val assign = AssignCaseService(cases, events)
    private val startReview = StartCaseReviewService(cases, events)
    private val resolve = ResolveCaseService(cases, events)
    private val reject = RejectCaseService(cases, events)

    private val reporterId = UUID.randomUUID()
    private val reportedId = UUID.randomUUID()
    private val moderatorId = UUID.randomUUID()

    private fun report(): UUID =
        intake.openCase(
            type = CaseType.REPORTE,
            subjectType = CaseSubjectType.CONVERSACION,
            subjectId = UUID.randomUUID(),
            reportedUserId = reportedId,
            openedByUserId = reporterId,
            description = "Conversación con acoso",
            priority = CasePriority.ALTA,
        )

    @Test
    fun `the intake opens the case and records the first traceability entry`() {
        val caseId = report()

        val detail = getCase.detail(caseId)

        assertEquals(CaseStatus.ABIERTO, detail.case.status)
        assertEquals(reportedId, detail.case.reportedUserId)
        assertEquals(listOf(CaseEventType.ABIERTO), detail.events.map { it.type })
        assertEquals(reporterId, detail.events.single().actorUserId)
    }

    @Test
    fun `the intake rejects an empty description and self-reports`() {
        assertFailsWith<InvalidArgumentException> {
            intake.openCase(
                CaseType.REPORTE, CaseSubjectType.USUARIO, UUID.randomUUID(),
                reportedId, reporterId, "  ", CasePriority.MEDIA,
            )
        }
        assertFailsWith<InvalidArgumentException> {
            intake.openCase(
                CaseType.DISPUTA, CaseSubjectType.EVALUACION, UUID.randomUUID(),
                reporterId, reporterId, "Auto disputa", CasePriority.MEDIA,
            )
        }
    }

    @Test
    fun `the moderator workflow is assign - review - resolve with notes`() {
        val caseId = report()

        assign.assign(AssignCaseCommand(caseId, moderatorId))
        startReview.start(StartCaseReviewCommand(caseId, moderatorId))
        val closed = resolve.resolve(ResolveCaseCommand(caseId, moderatorId, "Sanción aplicada al usuario"))

        assertEquals(CaseStatus.RESUELTO, closed.status)
        assertEquals(moderatorId, closed.assigneeUserId)
        assertTrue { closed.resolutionHours != null }
        assertEquals(
            listOf(CaseEventType.ABIERTO, CaseEventType.ASIGNADO, CaseEventType.EN_REVISION, CaseEventType.RESUELTO),
            getCase.detail(caseId).events.map { it.type },
        )
    }

    @Test
    fun `closing without the mandatory note is rejected`() {
        val caseId = report()

        assertFailsWith<InvalidArgumentException> {
            resolve.resolve(ResolveCaseCommand(caseId, moderatorId, "   "))
        }
        assertFailsWith<InvalidArgumentException> {
            reject.reject(RejectCaseCommand(caseId, moderatorId, ""))
        }
        assertEquals(CaseStatus.ABIERTO, cases.findById(caseId)!!.status)
    }

    @Test
    fun `a closed case admits no further decisions`() {
        val caseId = report()
        resolve.resolve(ResolveCaseCommand(caseId, moderatorId, "Cerrado"))

        assertFailsWith<ConflictException> {
            startReview.start(StartCaseReviewCommand(caseId, moderatorId))
        }
    }

    @Test
    fun `the queue filters by type, status and assignee`() {
        val reportId = report()
        val disputeId = intake.openCase(
            CaseType.DISPUTA, CaseSubjectType.EVALUACION, UUID.randomUUID(),
            UUID.randomUUID(), UUID.randomUUID(), "Evaluación injusta", CasePriority.BAJA,
        )
        assign.assign(AssignCaseCommand(reportId, moderatorId))

        assertEquals(2, listCases.search(CaseSearchQuery()).size)
        assertEquals(listOf(reportId), listCases.search(CaseSearchQuery(type = CaseType.REPORTE)).map { it.id })
        assertEquals(
            listOf(disputeId),
            listCases.search(CaseSearchQuery(type = CaseType.DISPUTA, onlyOpen = true)).map { it.id },
        )
        assertTrue { listCases.search(CaseSearchQuery(status = CaseStatus.RESUELTO)).isEmpty() }
        assertEquals(
            listOf(reportId),
            listCases.search(CaseSearchQuery(assigneeUserId = moderatorId)).map { it.id },
        )
    }

    @Test
    fun `the reporter follows up only their own cases`() {
        report()
        assertEquals(1, listCases.listOpenedBy(reporterId).size)
        assertTrue { listCases.listOpenedBy(UUID.randomUUID()).isEmpty() }
    }

    @Test
    fun `an unknown case is not found`() {
        assertFailsWith<NotFoundException> { getCase.detail(UUID.randomUUID()) }
        assertFailsWith<NotFoundException> {
            resolve.resolve(ResolveCaseCommand(UUID.randomUUID(), moderatorId, "Nada"))
        }
    }
}