package com.coliving.api.accommodation.presentation.controller

import com.coliving.api.accommodation.application.dto.AddPhotoCommand
import com.coliving.api.accommodation.application.dto.ConfigureCatalogReferencesCommand
import com.coliving.api.accommodation.application.dto.ConfigurePricingCommand
import com.coliving.api.accommodation.application.dto.CreatePropertyCommand
import com.coliving.api.accommodation.application.dto.CreatePublicationCommand
import com.coliving.api.accommodation.application.dto.CreateUnitCommand
import com.coliving.api.accommodation.application.dto.DeletePhotoCommand
import com.coliving.api.accommodation.application.dto.PricingView
import com.coliving.api.accommodation.application.dto.PropertyDetailView
import com.coliving.api.accommodation.application.dto.PropertyView
import com.coliving.api.accommodation.application.dto.PublicationView
import com.coliving.api.accommodation.application.dto.SetUnitAvailabilityCommand
import com.coliving.api.accommodation.application.dto.UnitView
import com.coliving.api.accommodation.application.dto.UpdatePropertyCommand
import com.coliving.api.accommodation.application.dto.UpdateUnitCommand
import com.coliving.api.accommodation.application.usecase.PropertyService
import com.coliving.api.accommodation.application.usecase.PublicationService
import com.coliving.api.accommodation.application.usecase.UnitService
import com.coliving.api.accommodation.presentation.dto.AddPhotoRequest
import com.coliving.api.accommodation.presentation.dto.ConfigureCatalogReferencesRequest
import com.coliving.api.accommodation.presentation.dto.ConfigurePricingRequest
import com.coliving.api.accommodation.presentation.dto.CreatePropertyRequest
import com.coliving.api.accommodation.presentation.dto.CreatePublicationRequest
import com.coliving.api.accommodation.presentation.dto.CreateUnitRequest
import com.coliving.api.accommodation.presentation.dto.ReorderPhotosRequest
import com.coliving.api.accommodation.presentation.dto.SetAvailabilityRequest
import com.coliving.api.accommodation.presentation.dto.UpdatePropertyRequest
import com.coliving.api.accommodation.presentation.dto.UpdateUnitRequest
import com.coliving.api.shared.security.CurrentUser
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Host endpoints (ANFITRION only) for monthly room rental. Ownership is
 * enforced inside the application services.
 */
@Tag(
    name = "Host Accommodation",
    description = "Properties, rooms, photographs, monthly pricing and commercial availability of the ANFITRION.",
)
@RestController
@RequestMapping("/api/v1/host")
class HostAccommodationController(
    private val propertyService: PropertyService,
    private val unitService: UnitService,
    private val publicationService: PublicationService,
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
                cityName = request.cityName,
                neighborhood = request.neighborhood,
                address = request.address,
                amenities = request.amenities,
            ),
        )
    }

    @GetMapping("/properties")
    fun listProperties(@AuthenticationPrincipal current: CurrentUser): List<PropertyView> =
        requireHost(current) { propertyService.listByHost(current.userId) }

    @GetMapping("/properties/{id}")
    fun propertyDetail(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): PropertyDetailView = requireHost(current) { propertyService.hostDetail(id, current.userId) }

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
                cityName = request.cityName,
                neighborhood = request.neighborhood,
                address = request.address,
                amenities = request.amenities ?: emptyList(),
            ),
        )
    }

    @PostMapping("/properties/{id}/photos")
    @ResponseStatus(HttpStatus.CREATED)
    fun addPropertyPhoto(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: AddPhotoRequest,
    ): PropertyView = requireHost(current) {
        propertyService.addPhoto(AddPhotoCommand(id, current.userId, request.url, request.position))
    }

    @DeleteMapping("/properties/{id}/photos/{photoId}")
    fun deletePropertyPhoto(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @PathVariable photoId: UUID,
    ): PropertyView = requireHost(current) {
        propertyService.deletePhoto(DeletePhotoCommand(id, photoId, current.userId))
    }

    @PutMapping("/properties/{id}/photos/order")
    fun reorderPropertyPhotos(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ReorderPhotosRequest,
    ): PropertyView = requireHost(current) {
        propertyService.reorderPhotos(id, current.userId, request.photoIds)
    }

    // ---------- Rooms ----------

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
                description = request.description,
                maxGuests = request.maxGuests,
                monthlyPrice = request.monthlyPrice,
                currency = request.currency,
                priceUnit = request.priceUnit,
                roomType = request.roomType,
                bathroomType = request.bathroomType,
                airConditioning = request.airConditioning,
                fan = request.fan,
                commonAreas = request.commonAreas,
                typeCode = request.typeCode,
                accessibilityCodes = request.accessibilityCodes.toSet(),
                sortOrder = request.sortOrder,
            ),
        )
    }

    @GetMapping("/properties/{id}/units")
    fun listUnits(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): List<UnitView> = requireHost(current) { unitService.listByProperty(id, current.userId) }

    @GetMapping("/units/{id}")
    fun getUnit(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): UnitView = requireHost(current) { unitService.findUnit(id, current.userId) }

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
                description = request.description,
                maxGuests = request.maxGuests,
                monthlyPrice = request.monthlyPrice,
                currency = request.currency,
                priceUnit = request.priceUnit,
                roomType = request.roomType,
                bathroomType = request.bathroomType,
                airConditioning = request.airConditioning,
                fan = request.fan,
                commonAreas = request.commonAreas,
                typeCode = request.typeCode,
                accessibilityCodes = request.accessibilityCodes?.toSet(),
                sortOrder = request.sortOrder,
            ),
        )
    }

    @PutMapping("/units/{id}/availability")
    fun setUnitAvailability(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: SetAvailabilityRequest,
    ): UnitView = requireHost(current) {
        unitService.setAvailability(
            SetUnitAvailabilityCommand(
                unitId = id,
                hostId = current.userId,
                enabled = request.enabled ?: false,
            ),
        )
    }

    @PostMapping("/units/{id}/photos")
    @ResponseStatus(HttpStatus.CREATED)
    fun addUnitPhoto(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: AddPhotoRequest,
    ): UnitView = requireHost(current) {
        unitService.addPhoto(AddPhotoCommand(id, current.userId, request.url, request.position))
    }

    @DeleteMapping("/units/{id}/photos/{photoId}")
    fun deleteUnitPhoto(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @PathVariable photoId: UUID,
    ): UnitView = requireHost(current) {
        unitService.deletePhoto(id, photoId, current.userId)
    }

    @PutMapping("/units/{id}/photos/order")
    fun reorderUnitPhotos(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ReorderPhotosRequest,
    ): UnitView = requireHost(current) {
        unitService.reorderPhotos(id, current.userId, request.photoIds)
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
                services = request.services.toSet(),
                applicableRuleCodes = request.applicableRuleCodes.toSet(),
            ),
        )
    }

    @PutMapping("/publications/{id}/catalog-references")
    fun configureCatalogReferences(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ConfigureCatalogReferencesRequest,
    ): PublicationView = requireHost(current) {
        publicationService.configureCatalogReferences(
            ConfigureCatalogReferencesCommand(
                publicationId = id,
                hostId = current.userId,
                services = request.services.orEmpty().toSet(),
                applicableRuleCodes = request.applicableRuleCodes.orEmpty().toSet(),
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

    @PostMapping("/publications/{id}/request-review")
    fun requestReview(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): PublicationView = requireHost(current) { publicationService.requestReview(id, current.userId) }

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
                monthlyPrice = request.monthlyPrice ?: error("monthlyPrice is required"),
                currency = request.currency ?: error("currency is required"),
                priceUnit = request.priceUnit,
            ),
        )
    }

    private fun <T> requireHost(current: CurrentUser, block: () -> T): T {
        if (!current.roleCodes.contains("ANFITRION")) {
            throw AccessDeniedException("ANFITRION role required")
        }
        return block()
    }
}
