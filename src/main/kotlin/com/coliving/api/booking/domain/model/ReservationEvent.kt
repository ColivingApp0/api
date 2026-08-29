package com.coliving.api.booking.domain.model

import com.coliving.api.booking.domain.enums.ReservationEventType
import java.time.Instant
import java.util.UUID

/**
 * Immutable history entry of a reservation (RF-042): every transition records
 * the event type, the acting user, an optional reason and the moment.
 */
class ReservationEvent(
    val id: UUID,
    val reservationId: UUID,
    val type: ReservationEventType,
    val actorUserId: UUID,
    val reason: String?,
    val occurredAt: Instant,
)
