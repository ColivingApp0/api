package com.coliving.api.booking.infrastructure.provider

import com.coliving.api.accommodation.application.port.out.UnitOccupancyPort
import com.coliving.api.booking.application.query.ReservationOccupancyQuery
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Adapter that lets accommodation read occupancy from booking. Occupancy is
 * always derived from accepted reservations (plus legacy CONFIRMADA rows), so
 * no independent counter can drift.
 */
@Component
class BookingOccupancyProvider(
    private val reservationOccupancyQuery: ReservationOccupancyQuery,
) : UnitOccupancyPort {

    override fun occupiedPlacesOf(unitIds: Collection<UUID>): Map<UUID, Int> =
        reservationOccupancyQuery.occupiedPlacesOf(unitIds)

    override fun residentIdsOf(unitId: UUID): List<UUID> =
        reservationOccupancyQuery.residentIdsOf(unitId)
}
