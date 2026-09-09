package com.coliving.api.booking.application.dto

import java.time.LocalDate
import java.util.UUID

/**
 * Parties of a reservation conversation (RF-050): the guest who requested the
 * stay and the host who owns the unit. Exposed through
 * [com.coliving.api.booking.application.query.ReservationMessagingQuery] so the
 * `messaging` context can build and authorize conversations without owning
 * reservation data.
 */
data class ReservationPartiesInfo(
    val reservationId: UUID,
    val guestUserId: UUID,
    val hostUserId: UUID,
    val fromDate: LocalDate,
    val toDate: LocalDate,
)