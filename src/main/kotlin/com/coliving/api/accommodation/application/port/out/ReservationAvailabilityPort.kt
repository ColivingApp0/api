package com.coliving.api.accommodation.application.port.out

import java.time.LocalDate
import java.util.UUID

/**
 * Application service that the future `booking` context will consume (through
 * an adapter), exposing the atomic range lock/release operations. It is the
 * single enforcement point for the no-double-booking invariant.
 */
interface ReservationAvailabilityPort {

    /** Atomically locks every day in [from, to); throws Conflict if any day is taken. */
    fun lockRange(unitId: UUID, from: LocalDate, to: LocalDate, reservationId: UUID)

    /** Releases the held days for a reservation. */
    fun unlockRange(unitId: UUID, from: LocalDate, to: LocalDate, reservationId: UUID)

    /**
     * Transitions every held day (BLOQUEADO by reservationId) to OCUPADO when
     * the reservation is confirmed. Part of the lifecycle defined in the SRS:
     * a locked provisional hold becomes an occupied confirmed stay.
     */
    fun confirmRange(unitId: UUID, from: LocalDate, to: LocalDate, reservationId: UUID)

    fun isRangeAvailable(unitId: UUID, from: LocalDate, to: LocalDate): Boolean
}