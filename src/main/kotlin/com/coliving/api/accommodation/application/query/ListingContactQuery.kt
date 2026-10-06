package com.coliving.api.accommodation.application.query

import java.util.UUID

/** Listing context attached to a conversation (RF-050, monthly-room variant). */
data class ListingContactInfo(
    val unitId: UUID,
    val publicationId: UUID,
    val hostId: UUID,
    val roomTitle: String,
    val propertyTitle: String,
    val cityName: String?,
)

/**
 * Read contract consumed by `messaging` to open a conversation about a room
 * before a reservation exists. The owner of the room is resolved here, so
 * messaging never reads accommodation tables.
 */
interface ListingContactQuery {
    fun findListing(unitId: UUID): ListingContactInfo?
}
