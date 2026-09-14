package com.coliving.api.booking.application.dto

import java.time.LocalDate
import java.util.UUID

/**
 * Parties and outcome of the reservation an evaluation is attached to. Exposed
 * through
 * [com.coliving.api.booking.application.query.ReservationEvaluationQuery] so the
 * `reputation` context can validate that only users linked by a valid relation
 * evaluate each other (RF-070), without reading booking tables.
 */
data class ReservationRelationInfo(
    val reservationId: UUID,
    val unitId: UUID,
    val guestUserId: UUID,
    val hostUserId: UUID,
    /** ReservationStatus name; only CONFIRMADA counts as an eligible stay. */
    val status: String,
    val fromDate: LocalDate,
    val toDate: LocalDate,
)