package com.coliving.api.search.application.dto

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/** Sort options (RF-032): price or recency; always deterministic. */
enum class ListingSort {
    PRICE_ASC,
    PRICE_DESC,
    RECENT,
}

data class SearchListingsCommand(
    val cityId: UUID? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
    val currency: String? = null,
    val availableFrom: LocalDate? = null,
    val availableTo: LocalDate? = null,
    val sort: ListingSort = ListingSort.RECENT,
    // Catalog-based filters (RF-031, RF-083).
    val services: Set<UUID>? = null,
    val roomTypeCode: UUID? = null,
    val accessibilityCode: UUID? = null,
    val minNights: Int? = null,
)

data class ListingView(
    val publicationId: UUID,
    val unitId: UUID,
    val title: String,
    val cityId: UUID?,
    val pricePerNight: BigDecimal?,
    val currency: String?,
    val serviceCodes: Set<UUID> = emptySet(),
    val roomTypeCode: UUID? = null,
    val accessibilityCodes: Set<UUID> = emptySet(),
    val minNights: Int? = null,
)
