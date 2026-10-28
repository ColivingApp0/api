package com.coliving.api.accommodation.presentation.controller

import com.coliving.api.accommodation.application.usecase.PropertyImageService
import com.coliving.api.accommodation.application.usecase.PropertyService
import com.coliving.api.accommodation.application.usecase.PublicationService
import com.coliving.api.shared.error.NotFoundException
import com.coliving.api.shared.security.CurrentUser
import java.util.UUID
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/properties")
class PropertyImageController(
    private val propertyService: PropertyService,
    private val propertyImageService: PropertyImageService,
    private val publicationService: PublicationService,
) {
    @GetMapping("/{propertyId}/images/{imageId}")
    fun getImage(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable propertyId: UUID,
        @PathVariable imageId: UUID,
    ): ResponseEntity<ByteArray> {
        val property = propertyService.findById(propertyId)
            ?: throw NotFoundException("Property image not found")
        val isOwner = property.hostId == current.userId
        if (!isOwner && !publicationService.hasPublishedListingForProperty(propertyId)) {
            throw NotFoundException("Property image not found")
        }

        val image = propertyImageService.content(propertyId, imageId)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.contentType))
            .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
            .body(image.bytes)
    }
}
