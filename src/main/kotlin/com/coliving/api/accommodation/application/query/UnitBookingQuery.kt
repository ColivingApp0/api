package com.coliving.api.accommodation.application.query

import com.coliving.api.accommodation.application.dto.UnitBookingInfo
import java.util.UUID

/**
 * Read contract that the `booking` bounded context consumes (through an
 * adapter) to validate and price reservations against accommodation data.
 * Mirrors identity's `UserVerificationQuery` pattern: the providing context
 * owns both the data and this projection.
 */
interface UnitBookingQuery {

    /**
     * Booking-relevant snapshot of a unit. Returns null when the unit or its
     * publication does not exist; the caller decides whether the publication
     * status admits requests (RN-03).
     */
    fun getUnitBookingInfo(unitId: UUID): UnitBookingInfo?

    /** Unit ids owned by a host (for listing incoming reservations). */
    fun unitIdsOfHost(hostId: UUID): List<UUID>

    /** Whether the unit belongs to a property owned by [hostId]. */
    fun isHostOfUnit(unitId: UUID, hostId: UUID): Boolean
}
