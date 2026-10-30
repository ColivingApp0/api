package com.coliving.api.accommodation.application.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Search-ready projection of a published room listing, consumed by `search`.
 * Only public data: no resident identities, no private contact details.
 */
data class PublishedListingInfo(
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
    val publishedAt: Instant,
    val serviceCodes: Set<UUID> = emptySet(),
    val accessibilityCodes: Set<UUID> = emptySet(),
)
