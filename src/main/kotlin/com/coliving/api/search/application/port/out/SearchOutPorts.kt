package com.coliving.api.search.application.port.out

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Published room facet read from `accommodation`. Public data only. */
data class ListingFacet(
    val publicationId: UUID,
    val unitId: UUID,
    val propertyId: UUID,
    val roomTitle: String,
    val propertyTitle: String,
    val description: String?,
    val cityId: UUID?,
    val cityName: String?,
    val neighborhood: String?,
    val monthlyPrice: BigDecimal?,
    val currency: String?,
    val priceUnit: String,
    val roomType: String,
    val bathroomType: String,
    val airConditioning: Boolean,
    val fan: Boolean,
    val capacity: Int,
    val occupiedPlaces: Int,
    val availablePlaces: Int,
    val photoUrl: String?,
    val propertyPhotoUrl: String?,
    val updatedAt: Instant,
    val serviceCodes: Set<UUID> = emptySet(),
    val accessibilityCodes: Set<UUID> = emptySet(),
)

interface PublicationCatalogPort {

    fun findPublishedListings(): List<ListingFacet>

    /** Whether the publication exists and is currently PUBLICADA (RF-034 guard). */
    fun isPublished(publicationId: UUID): Boolean
}
