package com.coliving.api.reputation.presentation.controller

import com.coliving.api.reputation.application.dto.CreateEvaluationCommand
import com.coliving.api.reputation.application.dto.DisputeEvaluationCommand
import com.coliving.api.reputation.application.dto.EvaluationView
import com.coliving.api.reputation.application.dto.ReputationView
import com.coliving.api.reputation.application.usecase.CreateEvaluationService
import com.coliving.api.reputation.application.usecase.DisputeEvaluationService
import com.coliving.api.reputation.application.usecase.GetReputationService
import com.coliving.api.reputation.application.usecase.ListEvaluationsService
import com.coliving.api.reputation.presentation.dto.CreateEvaluationRequest
import com.coliving.api.reputation.presentation.dto.DisputeEvaluationRequest
import com.coliving.api.shared.security.CurrentUser
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Evaluations between the parties of an eligible stay (RF-070) and the dispute
 * of an evaluation (RF-072). Any authenticated user may call them: eligibility
 * is decided by the application service from the relation read from booking, not
 * by a role.
 */
@Tag(
    name = "Reviews",
    description = "Evaluations between the parties of an eligible stay (RF-070) and the dispute of an evaluation (RF-072).",
)
@RestController
@RequestMapping("/api/v1/reviews")
class ReviewController(
    private val createEvaluationService: CreateEvaluationService,
    private val listEvaluationsService: ListEvaluationsService,
    private val disputeEvaluationService: DisputeEvaluationService,
) {

    /** Evaluates the counterpart of an eligible relation (the subject is derived). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal current: CurrentUser,
        @Valid @RequestBody request: CreateEvaluationRequest,
    ): EvaluationView = createEvaluationService.create(
        CreateEvaluationCommand(
            authorUserId = current.userId,
            relationType = request.relationType,
            relationId = request.relationId,
            ratings = request.ratings,
            comment = request.comment,
        ),
    )

    /** Evaluations the authenticated user wrote. */
    @GetMapping("/mine")
    fun listWritten(@AuthenticationPrincipal current: CurrentUser): List<EvaluationView> =
        listEvaluationsService.writtenBy(current.userId)

    /** Evaluations a user received; a reputation is public to authenticated users. */
    @GetMapping("/received/{userId}")
    fun listReceived(@PathVariable userId: UUID): List<EvaluationView> =
        listEvaluationsService.receivedBy(userId)

    /** Disputes an evaluation received; only the evaluated user can (RF-072). */
    @PostMapping("/{id}/dispute")
    fun dispute(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: DisputeEvaluationRequest,
    ): EvaluationView = disputeEvaluationService.dispute(
        DisputeEvaluationCommand(
            evaluationId = id,
            disputantUserId = current.userId,
            reason = request.reason,
        ),
    )
}

/**
 * Consultable reputation of a user (RF-071): score, the factors that influence
 * it, the version of the formula and the benefits the score grants (RF-073).
 */
@Tag(
    name = "Reputation",
    description = "Reputation score of a user (RF-071), public to authenticated users.",
)
@RestController
@RequestMapping("/api/v1/users")
class ReputationController(
    private val getReputationService: GetReputationService,
) {

    @GetMapping("/{userId}/reputation")
    fun reputation(@PathVariable userId: UUID): ReputationView =
        getReputationService.reputationOf(userId)
}