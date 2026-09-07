package com.coliving.api.accommodation.application.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Search-ready projection of a published listing, consumed by the `search`
 * context through [com.coliving.api.accommodation.application.query.PublicationSearchQuery].
 *
 * Only public-facing data (RN-02: exact location stays restricted — city only).
 */
data class PublishedListingInfo(
    val publicationId: UUID,
    val unitId: UUID,
    val title: String,
    val cityId: UUID?,
    val pricePerNight: BigDecimal?,
    val currency: String?,
    val publishedAt: Instant,
)
