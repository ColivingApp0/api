package com.coliving.api.booking.domain.repository

import com.coliving.api.booking.domain.model.Reservation
import com.coliving.api.booking.domain.model.ReservationEvent
import java.time.Instant
import java.util.UUID

interface ReservationRepository {
    fun findById(id: UUID): Reservation?
    fun save(reservation: Reservation)
    fun findByGuest(guestUserId: UUID): List<Reservation>

    /** Reservations over any of the given units (host inbox). */
    fun findByUnitIds(unitIds: List<UUID>): List<Reservation>

    /** ACEPTADA reservations whose provisional hold window elapsed before [now]. */
    fun findExpiredHolds(now: Instant): List<Reservation>
}

interface ReservationEventRepository {
    fun findByReservation(reservationId: UUID): List<ReservationEvent>
    fun save(event: ReservationEvent)
}
