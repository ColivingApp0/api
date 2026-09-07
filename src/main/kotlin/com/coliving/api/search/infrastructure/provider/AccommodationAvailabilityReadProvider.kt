package com.coliving.api.search.infrastructure.provider

import com.coliving.api.accommodation.application.port.out.ReservationAvailabilityPort
import com.coliving.api.search.application.port.out.UnitAvailabilityPort
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Adapter delegating the date-availability read to accommodation's enforcing
 * availability port (single source of truth for the inventory state).
 */
@Component
class AccommodationAvailabilityReadProvider(
    private val reservationAvailabilityPort: ReservationAvailabilityPort,
) : UnitAvailabilityPort {

    override fun isRangeAvailable(unitId: UUID, from: LocalDate, to: LocalDate): Boolean =
        reservationAvailabilityPort.isRangeAvailable(unitId, from, to)
}
