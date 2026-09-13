package com.coliving.api.moderation.presentation.controller

import com.coliving.api.moderation.application.dto.AssignCaseCommand
import com.coliving.api.moderation.application.dto.CaseDetailView
import com.coliving.api.moderation.application.dto.CaseSearchQuery
import com.coliving.api.moderation.application.dto.CaseView
import com.coliving.api.moderation.application.dto.ModerationMetricsView
import com.coliving.api.moderation.application.dto.RejectCaseCommand
import com.coliving.api.moderation.application.dto.ResolveCaseCommand
import com.coliving.api.moderation.application.dto.StartCaseReviewCommand
import com.coliving.api.moderation.application.usecase.AssignCaseService
import com.coliving.api.moderation.application.usecase.GetCaseService
import com.coliving.api.moderation.application.usecase.ListCasesService
import com.coliving.api.moderation.application.usecase.OperationsIndicatorsService
import com.coliving.api.moderation.application.usecase.RejectCaseService
import com.coliving.api.moderation.application.usecase.ResolveCaseService
import com.coliving.api.moderation.application.usecase.StartCaseReviewService
import com.coliving.api.moderation.domain.enums.CasePriority
import com.coliving.api.moderation.domain.enums.CaseStatus
import com.coliving.api.moderation.domain.enums.CaseType
import com.coliving.api.moderation.presentation.dto.ResolveCaseRequest
import com.coliving.api.moderation.presentation.dto.RejectCaseRequest
import com.coliving.api.shared.security.CurrentUser
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Support queue for the moderation team (RF-082). The admin namespace already
 * requires MODERADOR or ADMINISTRADOR (SecurityConfig), so the moderator
 * identity is taken from the authenticated principal.
 */
@Tag(
    name = "Admin Cases",
    description = "Support queue of the moderation team (RF-082): indicators, search, assignment and the review/resolve/reject chain; admin namespace.",
)
@RestController
@RequestMapping("/api/v1/admin/cases")
class AdminCaseController(
    private val listCasesService: ListCasesService,
    private val getCaseService: GetCaseService,
    private val assignCaseService: AssignCaseService,
    private val startCaseReviewService: StartCaseReviewService,
    private val resolveCaseService: ResolveCaseService,
    private val rejectCaseService: RejectCaseService,
    private val operationsIndicatorsService: OperationsIndicatorsService,
) {

    /** Operational indicators of the pilot's support queue (RF-084). */
    @GetMapping("/metrics")
    fun metrics(): ModerationMetricsView = operationsIndicatorsService.indicators()

    /** Queue with the filters the team needs; `onlyOpen` hides closed cases. */
    @GetMapping
    fun search(
        @RequestParam(required = false) type: CaseType?,
        @RequestParam(required = false) status: CaseStatus?,
        @RequestParam(required = false) priority: CasePriority?,
        @RequestParam(required = false) assigneeUserId: UUID?,
        @RequestParam(required = false, defaultValue = "false") onlyOpen: Boolean,
    ): List<CaseView> = listCasesService.search(
        CaseSearchQuery(
            type = type,
            status = status,
            priority = priority,
            assigneeUserId = assigneeUserId,
            onlyOpen = onlyOpen,
        ),
    )

    /** Case with its full traceability chain. */
    @GetMapping("/{id}")
    fun detail(@PathVariable id: UUID): CaseDetailView = getCaseService.detail(id)

    @PostMapping("/{id}/assign")
    fun assign(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): CaseView = assignCaseService.assign(AssignCaseCommand(caseId = id, moderatorId = current.userId))

    @PostMapping("/{id}/review")
    fun startReview(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): CaseView = startCaseReviewService.start(
        StartCaseReviewCommand(caseId = id, moderatorId = current.userId),
    )

    @PostMapping("/{id}/resolve")
    fun resolve(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ResolveCaseRequest,
    ): CaseView = resolveCaseService.resolve(
        ResolveCaseCommand(caseId = id, moderatorId = current.userId, resolution = request.resolution),
    )

    @PostMapping("/{id}/reject")
    fun reject(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: RejectCaseRequest,
    ): CaseView = rejectCaseService.reject(
        RejectCaseCommand(caseId = id, moderatorId = current.userId, reason = request.reason),
    )
}

/**
 * Follow-up for the user who raised a case: they see the state of their own
 * reports and disputes (RF-052, RF-072) without accessing anyone else's.
 */
@Tag(
    name = "Cases",
    description = "Follow-up for the user who raised a case: their own reports and disputes (RF-052, RF-072).",
)
@RestController
@RequestMapping("/api/v1/cases")
class MyCaseController(
    private val listCasesService: ListCasesService,
) {

    @GetMapping
    fun listMine(@AuthenticationPrincipal current: CurrentUser): List<CaseView> =
        listCasesService.listOpenedBy(current.userId)
}