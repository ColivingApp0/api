package com.coliving.api.accommodation.presentation.controller

import com.coliving.api.accommodation.application.dto.PublicationView
import com.coliving.api.accommodation.application.usecase.PublicationService
import com.coliving.api.accommodation.domain.enums.PublicationStatus
import com.coliving.api.accommodation.presentation.dto.ReviewPublicationRequest
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
 * Moderation queue for publications (RF-081). The admin namespace already
 * requires MODERADOR or ADMINISTRADOR (SecurityConfig), so the reviewer is the
 * authenticated principal. Reviews may happen before or after publication: the
 * queue defaults to the listings currently under review (EN_REVISION).
 */
@Tag(
    name = "Admin Publications",
    description = "Moderation queue of publications (RF-081); the admin namespace requires MODERADOR or ADMINISTRADOR.",
)
@RestController
@RequestMapping("/api/v1/admin/publications")
class AdminPublicationController(
    private val publicationService: PublicationService,
) {

    @GetMapping
    fun queue(
        @RequestParam(required = false) status: PublicationStatus?,
    ): List<PublicationView> =
        if (status == null) {
            publicationService.findForReview()
        } else {
            publicationService.findByStatusForAdmin(status)
        }

    @PostMapping("/{id}/review")
    fun review(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ReviewPublicationRequest,
    ): PublicationView = publicationService.resolveReview(
        publicationId = id,
        moderatorId = current.userId,
        decision = request.decision ?: error("decision is required"),
        note = request.note,
    )
}