package com.coliving.api.booking.application.query

import com.coliving.api.booking.application.dto.ReservationRelationInfo
import java.util.UUID

/**
 * Read contract that the `reputation` bounded context consumes (through an
 * adapter) to decide whether an evaluation is allowed and how many stays a user
 * completed (RF-070, RF-071). Mirrors `ReservationMessagingQuery`: booking owns
 * the reservations and this projection.
 */
interface ReservationEvaluationQuery {

    /**
     * Relation behind an evaluation: the guest and the host of the reservation,
     * or null when the reservation does not exist.
     */
    fun relationOf(reservationId: UUID): ReservationRelationInfo?

    /**
     * Confirmed stays of a user, as guest or as host. Feeds the documented
     * "completed stays" factor of the reputation score (RF-071).
     */
    fun confirmedStayCount(userId: UUID): Int
}