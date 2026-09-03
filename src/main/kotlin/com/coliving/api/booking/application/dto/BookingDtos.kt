package com.coliving.api.booking.application.dto

import com.coliving.api.booking.domain.enums.CancellationPolicyType
import com.coliving.api.booking.domain.enums.ReservationEventType
import com.coliving.api.booking.domain.enums.ReservationStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// ---------- Commands ----------

data class CreateReservationCommand(
    val guestId: UUID,
    val unitId: UUID,
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val occupants: Int,
    val message: String?,
)

data class HostDecisionCommand(
    val reservationId: UUID,
    val hostId: UUID,
    val reason: String?,
)

data class ConfirmReservationCommand(
    val reservationId: UUID,
    val guestId: UUID,
)

data class CancelReservationCommand(
    val reservationId: UUID,
    val actorId: UUID,
    val reason: String,
)

// ---------- Views ----------

data class ReservationView(
    val id: UUID,
    val unitId: UUID,
    val publicationId: UUID,
    val guestUserId: UUID,
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val occupants: Int,
    val message: String?,
    val status: ReservationStatus,
    val pricePerNight: BigDecimal,
    val currency: String,
    val totalPrice: BigDecimal,
    val cancellationPolicy: CancellationPolicyType?,
    val holdExpiresAt: Instant?,
    val createdAt: Instant,
)

data class ReservationEventView(
    val id: UUID,
    val reservationId: UUID,
    val type: ReservationEventType,
    val actorUserId: UUID,
    val reason: String?,
    val occurredAt: Instant,
)
