package com.coliving.api.community.presentation.controller

import com.coliving.api.community.application.dto.ActivityDetailView
import com.coliving.api.community.application.dto.ActivityView
import com.coliving.api.community.application.dto.CommunitySummaryView
import com.coliving.api.community.application.dto.ConfirmAttendanceCommand
import com.coliving.api.community.application.dto.ParticipantView
import com.coliving.api.community.application.dto.WithdrawFromActivityCommand
import com.coliving.api.community.application.usecase.CommunitySummaryService
import com.coliving.api.community.application.usecase.ConfirmAttendanceService
import com.coliving.api.community.application.usecase.ListActivitiesService
import com.coliving.api.community.application.usecase.WithdrawFromActivityService
import com.coliving.api.shared.security.CurrentUser
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Community endpoints for residents (RF-061, RF-062). Any authenticated user
 * may call them: whether the user belongs to the authorized community of an
 * activity is decided by the application service from the enabled participants,
 * not by a role.
 */
@Tag(
    name = "Community",
    description = "Community activities for residents (RF-061, RF-062); available to any authenticated user.",
)
@RestController
@RequestMapping("/api/v1/community")
class CommunityActivityController(
    private val listActivitiesService: ListActivitiesService,
    private val confirmAttendanceService: ConfirmAttendanceService,
    private val withdrawFromActivityService: WithdrawFromActivityService,
    private val communitySummaryService: CommunitySummaryService,
) {

    /** Activities the authenticated user was enabled in. */
    @GetMapping("/activities")
    fun listMine(@AuthenticationPrincipal current: CurrentUser): List<ActivityView> =
        listActivitiesService.listMine(current.userId)

    @GetMapping("/activities/{id}")
    fun detail(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): ActivityDetailView = listActivitiesService.detail(id, current.userId)

    @PostMapping("/activities/{id}/confirm")
    fun confirm(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): ParticipantView = confirmAttendanceService.confirm(
        ConfirmAttendanceCommand(activityId = id, userId = current.userId),
    )

    @PostMapping("/activities/{id}/withdraw")
    fun withdraw(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): ParticipantView = withdrawFromActivityService.withdraw(
        WithdrawFromActivityCommand(activityId = id, userId = current.userId),
    )

    /** Aggregated (consent-gated) view of the resident community of a property. */
    @GetMapping("/summary")
    fun summary(@RequestParam("propertyId") propertyId: UUID): CommunitySummaryView =
        communitySummaryService.summaryOf(propertyId)
}