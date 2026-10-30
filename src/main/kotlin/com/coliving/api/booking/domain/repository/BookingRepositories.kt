package com.coliving.api.booking.domain.repository

import com.coliving.api.booking.domain.enums.ReservationStatus
import com.coliving.api.booking.domain.model.Reservation
import com.coliving.api.booking.domain.model.ReservationEvent
import java.util.UUID

interface ReservationRepository {
    fun findById(id: UUID): Reservation?
    fun save(reservation: Reservation)
    fun findByGuest(guestUserId: UUID): List<Reservation>

    /** Reservations over any of the given rooms (host inbox). */
    fun findByUnitIds(unitIds: List<UUID>): List<Reservation>

    /** Occupied places per room (accepted/confirmed requests). */
    fun occupiedPlacesOf(unitIds: Collection<UUID>): Map<UUID, Int>

    /** Guest ids currently occupying the room (accepted/confirmed). */
    fun residentIdsOf(unitId: UUID): List<UUID>

    /** Whether the guest already has an active request for the room. */
    fun hasActiveRequest(guestUserId: UUID, unitId: UUID): Boolean
}

interface ReservationEventRepository {
    fun findByReservation(reservationId: UUID): List<ReservationEvent>
    fun save(event: ReservationEvent)
}

/** Statuses whose places are occupied. Legacy CONFIRMADA is included on purpose. */
val OCCUPYING_STATUSES: List<ReservationStatus> =
    listOf(ReservationStatus.ACEPTADA, ReservationStatus.CONFIRMADA)

/** Statuses that prevent a second active request by the same guest on a room. */
val ACTIVE_STATUSES: List<ReservationStatus> =
    listOf(ReservationStatus.SOLICITADA, ReservationStatus.INFORMACION_SOLICITADA, ReservationStatus.ACEPTADA, ReservationStatus.CONFIRMADA)
