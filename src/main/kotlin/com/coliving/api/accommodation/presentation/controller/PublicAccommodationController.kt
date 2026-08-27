package com.coliving.api.accommodation.presentation.controller

import com.coliving.api.accommodation.application.dto.DayAvailabilityView
import com.coliving.api.accommodation.application.dto.PublicationDetailView
import com.coliving.api.accommodation.application.dto.PublicationView
import com.coliving.api.accommodation.application.usecase.AvailabilityLockService
import com.coliving.api.accommodation.application.usecase.PublicationService
import java.time.LocalDate
import java.util.UUID
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Read endpoints for any authenticated user. Only PUBLICADA publications are
 * exposed.
 */
@RestController
@RequestMapping("/api/v1/publications")
class PublicAccommodationController(
    private val publicationService: PublicationService,
    private val availabilityLockService: AvailabilityLockService,
) {

    @GetMapping
    fun list(): List<PublicationView> = publicationService.findAllPublished()

    @GetMapping("/{id}")
    fun detail(@PathVariable id: UUID): PublicationDetailView =
        publicationService.findPublishedDetail(id)
            ?: throw com.coliving.api.shared.error.NotFoundException("Published publication not found")

    @GetMapping("/{id}/availability")
    fun availability(
        @PathVariable id: UUID,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): List<DayAvailabilityView> {
        val publication = publicationService.findPublishedDetail(id)
            ?: throw com.coliving.api.shared.error.NotFoundException("Published publication not found")
        return availabilityLockService.getAvailability(publication.publication.unitId, from, to)
    }
}