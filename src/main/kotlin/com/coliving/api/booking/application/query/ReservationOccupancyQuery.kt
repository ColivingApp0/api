package com.coliving.api.booking.application.query

import java.util.UUID

/**
 * Occupancy read contract consumed by the `accommodation` context through its
 * UnitOccupancyPort. Occupancy is always derived from accepted reservations, so
 * there is no duplicated counter to drift.
 */
interface ReservationOccupancyQuery {

    fun occupiedPlacesOf(unitIds: Collection<UUID>): Map<UUID, Int>

    fun residentIdsOf(unitId: UUID): List<UUID>
}
