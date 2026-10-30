package com.coliving.api.accommodation.application.query

import com.coliving.api.accommodation.application.dto.UnitBookingInfo
import java.util.UUID

/**
 * Read contract that the `booking` bounded context consumes to validate and
 * price monthly room requests against accommodation data.
 */
interface UnitBookingQuery {

    /**
     * Booking-relevant snapshot of a room. Returns null when the room or its
     * publication does not exist; the caller decides whether the publication
     * status admits requests (RN-03).
     */
    fun getUnitBookingInfo(unitId: UUID): UnitBookingInfo?

    /**
     * Same snapshot but locking the room row (`SELECT ... FOR UPDATE`) for the
     * rest of the caller's transaction. Used when accepting a request, so two
     * concurrent accepts cannot take the same last place.
     */
    fun lockUnitBookingInfo(unitId: UUID): UnitBookingInfo?

    /** Room ids owned by a host (for listing incoming reservations). */
    fun unitIdsOfHost(hostId: UUID): List<UUID>

    /** Whether the room belongs to a property owned by [hostId]. */
    fun isHostOfUnit(unitId: UUID, hostId: UUID): Boolean

    /** Owner of the room, or null when the room (or its property) does not exist. */
    fun hostOfUnit(unitId: UUID): UUID?
}
