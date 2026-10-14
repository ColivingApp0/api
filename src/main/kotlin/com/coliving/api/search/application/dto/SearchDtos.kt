package com.coliving.api.search.application.dto

import java.math.BigDecimal
import java.util.UUID

/** Sort options (RF-032): monthly price or recency; always deterministic. */
enum class ListingSort {
    PRICE_ASC,
    PRICE_DESC,
    RECENT,
}

data class SearchListingsCommand(
    val q: String? = null,
    val cityId: UUID? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
    val currency: String? = null,
    val sort: ListingSort = ListingSort.RECENT,
    val minCapacity: Int? = null,
    val availableOnly: Boolean = true,
    val airConditioning: Boolean? = null,
    val fan: Boolean? = null,
    val bathroomType: String? = null,
    val roomType: String? = null,
    val propertyId: UUID? = null,
    val services: Set<UUID>? = null,
    val accessibilityCode: UUID? = null,
)

/** Monthly room search result. No resident identity is ever exposed here. */
data class ListingView(
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
    val serviceCodes: Set<UUID> = emptySet(),
    val accessibilityCodes: Set<UUID> = emptySet(),
    val available: Boolean,
    val availabilityLabel: String,
)
