package com.coliving.api.booking.application.dto

import java.util.UUID

/**
 * Parties of a reservation conversation (RF-050): the guest who requested the
 * place and the host who owns the room, plus the listing context.
 */
data class ReservationPartiesInfo(
    val reservationId: UUID,
    val unitId: UUID,
    val publicationId: UUID,
    val guestUserId: UUID,
    val hostUserId: UUID,
)
