package com.coliving.api.community.presentation.controller

import com.coliving.api.community.application.dto.ActivityDetailView
import com.coliving.api.community.application.dto.ActivityView
import com.coliving.api.community.application.dto.CancelActivityCommand
import com.coliving.api.community.application.dto.CreateActivityCommand
import com.coliving.api.community.application.usecase.CancelActivityService
import com.coliving.api.community.application.usecase.CreateActivityService
import com.coliving.api.community.application.usecase.ListActivitiesService
import com.coliving.api.community.presentation.dto.CreateActivityRequest
import com.coliving.api.shared.security.CurrentUser
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Host endpoints for the community of their properties (RF-060): create an
 * activity for the authorized residents, list the activities of their
 * properties and inspect who is taking part.
 */
@Tag(
    name = "Host Community",
    description = "Activities created by the ANFITRION for the authorized residents of their properties (RF-060).",
)
@RestController
@RequestMapping("/api/v1/host/community/activities")
class HostCommunityController(
    private val createActivityService: CreateActivityService,
    private val cancelActivityService: CancelActivityService,
    private val listActivitiesService: ListActivitiesService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal current: CurrentUser,
        @Valid @RequestBody request: CreateActivityRequest,
    ): ActivityDetailView = requireHost(current) {
        createActivityService.create(
            CreateActivityCommand(
                hostId = current.userId,
                propertyId = request.propertyId,
                title = request.title,
                description = request.description,
                scheduledAt = request.scheduledAt,
                capacity = request.capacity,
                enabledParticipantIds = request.enabledParticipantIds,
            ),
        )
    }

    @GetMapping
    fun listMine(@AuthenticationPrincipal current: CurrentUser): List<ActivityView> =
        requireHost(current) { listActivitiesService.listForHost(current.userId) }

    @GetMapping("/{id}")
    fun detail(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): ActivityDetailView = requireHost(current) { listActivitiesService.detail(id, current.userId) }

    @PostMapping("/{id}/cancel")
    fun cancel(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): ActivityView = requireHost(current) {
        cancelActivityService.cancel(CancelActivityCommand(activityId = id, actorId = current.userId))
    }

    private fun <T> requireHost(current: CurrentUser, block: () -> T): T {
        if (!current.roleCodes.contains("ANFITRION")) {
            throw AccessDeniedException("ANFITRION role required")
        }
        return block()
    }
}