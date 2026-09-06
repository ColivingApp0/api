package com.coliving.api.search.application.port.out

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Published-listing facet read from `accommodation` (see
 * PublicationSearchQuery). Only public data: exact location stays restricted
 * (RN-02) — the city facet is a catalog reference, not an address.
 */
data class ListingFacet(
    val publicationId: UUID,
    val unitId: UUID,
    val title: String,
    val cityId: UUID?,
    val pricePerNight: BigDecimal?,
    val currency: String?,
    val updatedAt: Instant,
)

interface PublicationCatalogPort {

    fun findPublishedListings(): List<ListingFacet>

    /** Whether the publication exists and is currently PUBLICADA (RF-034 guard). */
    fun isPublished(publicationId: UUID): Boolean
}

/**
 * Availability read used by the date filter (RF-030): whether a unit is free
 * for the whole [from, to) range. Implemented through accommodation's
 * enforcing availability port.
 */
interface UnitAvailabilityPort {

    fun isRangeAvailable(unitId: UUID, from: java.time.LocalDate, to: java.time.LocalDate): Boolean
}
