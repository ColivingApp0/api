package com.coliving.api.accommodation.presentation.controller

import com.coliving.api.accommodation.application.dto.PropertyDetailView
import com.coliving.api.accommodation.application.dto.PublicationDetailView
import com.coliving.api.accommodation.application.dto.PublicationView
import com.coliving.api.accommodation.application.dto.RoommateView
import com.coliving.api.accommodation.application.usecase.PropertyService
import com.coliving.api.accommodation.application.usecase.PublicationService
import com.coliving.api.accommodation.application.usecase.ReportPublicationService
import com.coliving.api.accommodation.presentation.dto.ReportPublicationRequest
import com.coliving.api.shared.error.NotFoundException
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
 * Read endpoints for authenticated guests. Only published rooms are exposed.
 * Resident identities are never part of these public payloads: roommates are a
 * separate, consent-filtered read.
 */
@Tag(
    name = "Properties and rooms",
    description = "Published properties and rooms for guests (RF-033, monthly rental).",
)
@RestController
@RequestMapping("/api/v1")
class PublicAccommodationController(
    private val publicationService: PublicationService,
    private val propertyService: PropertyService,
    private val reportPublicationService: ReportPublicationService,
) {

    @GetMapping("/publications")
    fun list(): List<PublicationView> = publicationService.findAllPublished()

    @GetMapping("/publications/{id}")
    fun detail(@PathVariable id: UUID): PublicationDetailView =
        publicationService.findPublishedDetail(id)
            ?: throw NotFoundException("Habitación publicada no encontrada")

    /** Consented roommates of a shared room (never on public search payloads). */
    @GetMapping("/publications/{id}/roommates")
    fun roommates(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): List<RoommateView> = publicationService.roommates(id)

    @GetMapping("/properties/{id}")
    fun property(@PathVariable id: UUID): PropertyDetailView =
        propertyService.publicDetail(id)
            ?: throw NotFoundException("Propiedad no encontrada")

    @PostMapping("/publications/{id}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    fun report(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ReportPublicationRequest,
    ): Map<String, UUID> = mapOf(
        "caseId" to reportPublicationService.report(
            publicationId = id,
            reporterUserId = current.userId,
            reason = request.reason,
        ),
    )
}
