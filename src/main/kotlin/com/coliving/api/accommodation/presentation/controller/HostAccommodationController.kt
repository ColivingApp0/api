package com.coliving.api.accommodation.presentation.controller

import com.coliving.api.accommodation.application.dto.BlockDatesCommand
import com.coliving.api.accommodation.application.dto.CreatePropertyCommand
import com.coliving.api.accommodation.application.dto.CreatePublicationCommand
import com.coliving.api.accommodation.application.dto.CreateUnitCommand
import com.coliving.api.accommodation.application.dto.DayAvailabilityView
import com.coliving.api.accommodation.application.dto.PropertyView
import com.coliving.api.accommodation.application.dto.PricingView
import com.coliving.api.accommodation.application.dto.PublicationView
import com.coliving.api.accommodation.application.dto.RulesView
import com.coliving.api.accommodation.application.dto.UnitView
import com.coliving.api.accommodation.application.dto.UnblockDatesCommand
import com.coliving.api.accommodation.application.dto.UpdatePropertyCommand
import com.coliving.api.accommodation.application.dto.UpdateUnitCommand
import com.coliving.api.accommodation.application.dto.ConfigurePricingCommand
import com.coliving.api.accommodation.application.dto.ConfigureRulesCommand
import com.coliving.api.accommodation.application.usecase.AvailabilityLockService
import com.coliving.api.accommodation.application.usecase.PropertyService
import com.coliving.api.accommodation.application.usecase.PublicationService
import com.coliving.api.accommodation.application.usecase.UnitService
import com.coliving.api.accommodation.presentation.dto.ConfigurePricingRequest
import com.coliving.api.accommodation.presentation.dto.ConfigureRulesRequest
import com.coliving.api.accommodation.presentation.dto.CreatePropertyRequest
import com.coliving.api.accommodation.presentation.dto.CreatePublicationRequest
import com.coliving.api.accommodation.presentation.dto.CreateUnitRequest
import com.coliving.api.accommodation.presentation.dto.UpdatePropertyRequest
import com.coliving.api.accommodation.presentation.dto.UpdateUnitRequest
import com.coliving.api.shared.security.CurrentUser
import jakarta.validation.Valid
import java.time.LocalDate
import java.util.UUID
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Host endpoints (ANFITRION only). The role is checked via the shared
 * principal; ownership is enforced inside the application services.
 */
@RestController
@RequestMapping("/api/v1/host")
class HostAccommodationController(
    private val propertyService: PropertyService,
    private val unitService: UnitService,
    private val publicationService: PublicationService,
    private val availabilityLockService: AvailabilityLockService,
) {

    // ---------- Properties ----------

    @PostMapping("/properties")
    @ResponseStatus(HttpStatus.CREATED)
    fun createProperty(
        @AuthenticationPrincipal current: CurrentUser,
        @Valid @RequestBody request: CreatePropertyRequest,
    ): PropertyView = requireHost(current) {
        propertyService.create(
            CreatePropertyCommand(
                hostId = current.userId,
                title = request.title,
                description = request.description,
                cityId = request.cityId,
                address = request.address,
                amenities = request.amenities,
            ),
        )
    }

    @GetMapping("/properties")
    fun listProperties(@AuthenticationPrincipal current: CurrentUser): List<PropertyView> =
        requireHost(current) { propertyService.listByHost(current.userId) }

    @PatchMapping("/properties/{id}")
    fun updateProperty(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdatePropertyRequest,
    ): PropertyView = requireHost(current) {
        propertyService.update(
            UpdatePropertyCommand(
                propertyId = id,
                hostId = current.userId,
                title = request.title,
                description = request.description,
                cityId = request.cityId,
                address = request.address,
                amenities = request.amenities ?: emptyList(),
            ),
        )
    }

    // ---------- Units ----------

    @PostMapping("/properties/{id}/units")
    @ResponseStatus(HttpStatus.CREATED)
    fun createUnit(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateUnitRequest,
    ): UnitView = requireHost(current) {
        unitService.create(
            CreateUnitCommand(
                propertyId = id,
                hostId = current.userId,
                name = request.name,
                maxGuests = request.maxGuests,
                bedrooms = request.bedrooms,
                beds = request.beds,
                bathrooms = request.bathrooms,
            ),
        )
    }

    @GetMapping("/properties/{id}/units")
    fun listUnits(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): List<UnitView> = requireHost(current) { unitService.listByProperty(id) }

    @PatchMapping("/units/{id}")
    fun updateUnit(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateUnitRequest,
    ): UnitView = requireHost(current) {
        unitService.update(
            UpdateUnitCommand(
                unitId = id,
                hostId = current.userId,
                name = request.name,
                maxGuests = request.maxGuests,
                bedrooms = request.bedrooms,
                beds = request.beds,
                bathrooms = request.bathrooms,
            ),
        )
    }

    // ---------- Publications ----------

    @PostMapping("/units/{id}/publication")
    @ResponseStatus(HttpStatus.CREATED)
    fun createPublication(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreatePublicationRequest,
    ): PublicationView = requireHost(current) {
        publicationService.createPublication(
            CreatePublicationCommand(
                unitId = id,
                hostId = current.userId,
                title = request.title,
            ),
        )
    }

    @PostMapping("/publications/{id}/publish")
    fun publish(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): PublicationView = requireHost(current) { publicationService.publish(id, current.userId) }

    @PostMapping("/publications/{id}/pause")
    fun pause(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): PublicationView = requireHost(current) { publicationService.pause(id, current.userId) }

    @PostMapping("/publications/{id}/hide")
    fun hide(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): PublicationView = requireHost(current) { publicationService.hide(id, current.userId) }

    @PostMapping("/publications/{id}/archive")
    fun archive(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): PublicationView = requireHost(current) { publicationService.archive(id, current.userId) }

    @PutMapping("/publications/{id}/pricing")
    fun configurePricing(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ConfigurePricingRequest,
    ): PricingView = requireHost(current) {
        publicationService.configurePricing(
            ConfigurePricingCommand(
                publicationId = id,
                hostId = current.userId,
                basePricePerNight = request.basePricePerNight ?: error("basePricePerNight is required"),
                currency = request.currency ?: error("currency is required"),
            ),
        )
    }

    @PutMapping("/publications/{id}/rules")
    fun configureRules(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ConfigureRulesRequest,
    ): RulesView = requireHost(current) {
        publicationService.configureRules(
            ConfigureRulesCommand(
                publicationId = id,
                hostId = current.userId,
                cancellationPolicy = request.cancellationPolicy ?: error("cancellationPolicy is required"),
                checkInTime = request.checkInTime ?: error("checkInTime is required"),
                checkOutTime = request.checkOutTime ?: error("checkOutTime is required"),
                minNights = request.minNights,
                maxNights = request.maxNights,
                houseRules = request.houseRules,
            ),
        )
    }

    // ---------- Availability (host blocks) ----------

    @PutMapping("/units/{id}/availability/blocks")
    fun blockDates(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): List<DayAvailabilityView> = requireHost(current) {
        availabilityLockService.blockDates(
            BlockDatesCommand(unitId = id, hostId = current.userId, from = from, to = to),
        )
        availabilityLockService.getAvailability(id, from, to)
    }

    @DeleteMapping("/units/{id}/availability/blocks")
    fun unblockDates(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): List<DayAvailabilityView> = requireHost(current) {
        availabilityLockService.unblockDates(
            UnblockDatesCommand(unitId = id, hostId = current.userId, from = from, to = to),
        )
        availabilityLockService.getAvailability(id, from, to)
    }

    private fun <T> requireHost(current: CurrentUser, block: () -> T): T {
        if (!current.roleCodes.contains("ANFITRION")) {
            throw AccessDeniedException("ANFITRION role required")
        }
        return block()
    }
}