package com.coliving.api.booking.application.dto

import java.util.UUID

/**
 * Parties and outcome of the reservation an evaluation is attached to (RF-070).
 * A stay is eligible when its request was accepted (ACEPTADA, or legacy
 * CONFIRMADA).
 */
data class ReservationRelationInfo(
    val reservationId: UUID,
    val unitId: UUID,
    val guestUserId: UUID,
    val hostUserId: UUID,
    val status: String,
)
