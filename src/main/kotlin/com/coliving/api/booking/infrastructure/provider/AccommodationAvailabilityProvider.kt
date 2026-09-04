package com.coliving.api.booking.infrastructure.provider

import com.coliving.api.booking.application.port.out.UnitAvailabilityPort
import com.coliving.api.accommodation.application.port.out.ReservationAvailabilityPort
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Adapter from booking's availability port to accommodation's enforcing
 * ReservationAvailabilityPort. All locking semantics (atomicity, ordering,
 * no-double-booking) live in the provider; booking only states intent.
 */
@Component
class AccommodationAvailabilityProvider(
    private val reservationAvailabilityPort: ReservationAvailabilityPort,
) : UnitAvailabilityPort {

    override fun isRangeAvailable(unitId: UUID, from: LocalDate, to: LocalDate): Boolean =
        reservationAvailabilityPort.isRangeAvailable(unitId, from, to)

    override fun lockRange(unitId: UUID, from: LocalDate, to: LocalDate, reservationId: UUID) =
        reservationAvailabilityPort.lockRange(unitId, from, to, reservationId)

    override fun unlockRange(unitId: UUID, from: LocalDate, to: LocalDate, reservationId: UUID) =
        reservationAvailabilityPort.unlockRange(unitId, from, to, reservationId)

    override fun confirmRange(unitId: UUID, from: LocalDate, to: LocalDate, reservationId: UUID) =
        reservationAvailabilityPort.confirmRange(unitId, from, to, reservationId)
}
