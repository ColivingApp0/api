package com.coliving.api.messaging.application.port.out

import java.time.LocalDate
import java.util.UUID

/**
 * Parties of a reservation as read from `booking` (see
 * ReservationMessagingQuery). Kept as a local projection so messaging does not
 * depend on booking's DTOs, mirroring accommodation's BookableUnit.
 */
data class ReservationParties(
    val reservationId: UUID,
    val guestUserId: UUID,
    val hostUserId: UUID,
    val fromDate: LocalDate,
    val toDate: LocalDate,
)

/**
 * Reservation read contract consumed from `booking` (RF-050): who may
 * converse inside a reservation. Implemented by an adapter delegating to
 * booking's ReservationMessagingQuery.
 */
interface ReservationPartiesPort {

    fun findParties(reservationId: UUID): ReservationParties?
}
