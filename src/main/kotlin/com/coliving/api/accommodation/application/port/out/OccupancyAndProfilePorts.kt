package com.coliving.api.accommodation.application.port.out

import java.util.UUID

/**
 * Occupancy read contract. Implemented by the `booking` context (owner of the
 * accepted reservations) so accommodation never keeps a duplicated counter that
 * could drift from the real records.
 */
interface UnitOccupancyPort {

    /** Occupied places per room. Missing rooms have zero occupied places. */
    fun occupiedPlacesOf(unitIds: Collection<UUID>): Map<UUID, Int>

    /** Guests currently occupying a place in the room (accepted reservations). */
    fun residentIdsOf(unitId: UUID): List<UUID>
}

/** Public profile projection consumed from the `profile` context. */
data class PublicProfileCard(
    val userId: UUID,
    val name: String?,
    val avatarUrl: String?,
    /** Explicit authorization to show the profile to potential roommates. */
    val roommateProfileVisible: Boolean,
)

interface PublicProfilePort {
    fun findPublicProfile(userId: UUID): PublicProfileCard?
}
