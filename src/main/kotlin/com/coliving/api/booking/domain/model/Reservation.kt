package com.coliving.api.booking.domain.model

import com.coliving.api.booking.domain.enums.CancellationPolicyType
import com.coliving.api.booking.domain.enums.ReservationStatus
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.InvalidArgumentException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Reservation aggregate (SRS "Solicitudes", RF-040..RF-044).
 *
 * A reservation references a unit and a guest by plain UUID (no cross-context
 * relations). Economic conditions are frozen at creation (RN-07): price per
 * night, currency and cancellation policy never change after that.
 *
 * State machine (only these methods mutate [status]):
 * - created as [ReservationStatus.SOLICITADA]
 * - host: [accept] (locks the range, starts the provisional hold), [reject],
 *   [requestInfo]
 * - guest: [confirm] (inside the hold window)
 * - both: [cancel] (reason required, RF-044)
 * - system: [expire] (hold window elapsed, RF-043)
 */
class Reservation internal constructor(
    val id: UUID,
    val unitId: UUID,
    val publicationId: UUID,
    val guestUserId: UUID,
    val fromDate: LocalDate,
    val toDate: LocalDate,
    occupants: Int,
    message: String?,
    status: ReservationStatus,
    val pricePerNight: BigDecimal,
    val currency: String,
    val cancellationPolicy: CancellationPolicyType?,
    holdExpiresAt: Instant?,
    val createdAt: Instant,
) {

    var occupants: Int = occupants
        private set

    var message: String? = message
        private set

    var status: ReservationStatus = status
        private set

    var holdExpiresAt: Instant? = holdExpiresAt
        private set

    var updatedAt: Instant = createdAt
        private set

    /** Frozen economic total (RN-07): nights × snapshot price. */
    val nights: Long
        get() = ChronoUnit.DAYS.between(fromDate, toDate)

    val totalPrice: BigDecimal
        get() = pricePerNight.multiply(BigDecimal.valueOf(nights))

    /** Host accepts: the unit's days are locked and a provisional hold starts (RF-043). */
    fun accept(holdExpiresAt: Instant, now: Instant) {
        requireStatus("accept", ReservationStatus.SOLICITADA, ReservationStatus.INFORMACION_SOLICITADA)
        status = ReservationStatus.ACEPTADA
        this.holdExpiresAt = holdExpiresAt
        touch(now)
    }

    /** Host rejects: terminal. No days are held at this point. */
    fun reject(now: Instant) {
        requireStatus("reject", ReservationStatus.SOLICITADA, ReservationStatus.INFORMACION_SOLICITADA)
        status = ReservationStatus.RECHAZADA
        touch(now)
    }

    /** Host asks the guest for more information before deciding (RF-041). */
    fun requestInfo(now: Instant) {
        requireStatus("request info", ReservationStatus.SOLICITADA)
        status = ReservationStatus.INFORMACION_SOLICITADA
        touch(now)
    }

    /** Guest confirms inside the provisional window: the hold becomes an occupied stay. */
    fun confirm(now: Instant) {
        requireStatus("confirm", ReservationStatus.ACEPTADA)
        val deadline = holdExpiresAt
            ?: throw ConflictException("Reservation has no provisional hold window")
        if (now.isAfter(deadline)) {
            throw ConflictException("Provisional hold expired at $deadline")
        }
        status = ReservationStatus.CONFIRMADA
        touch(now)
    }

    /** Guest or host cancels with a mandatory reason (RF-044). */
    fun cancel(now: Instant) {
        requireStatus(
            "cancel",
            ReservationStatus.SOLICITADA,
            ReservationStatus.INFORMACION_SOLICITADA,
            ReservationStatus.ACEPTADA,
            ReservationStatus.CONFIRMADA,
        )
        status = ReservationStatus.CANCELADA
        touch(now)
    }

    /** System expiry of an unanswered provisional hold (RF-043). */
    fun expire(now: Instant) {
        requireStatus("expire", ReservationStatus.ACEPTADA)
        status = ReservationStatus.EXPIRADA
        touch(now)
    }

    /** Whether the unit's days are currently held/occupied on behalf of this reservation. */
    val holdsInventory: Boolean
        get() = status in setOf(ReservationStatus.ACEPTADA, ReservationStatus.CONFIRMADA)

    private fun requireStatus(action: String, vararg allowed: ReservationStatus) {
        if (status !in allowed) {
            throw ConflictException(
                "Cannot $action a reservation in status $status (allowed: ${allowed.joinToString()})",
            )
        }
    }

    private fun touch(now: Instant) {
        updatedAt = now
    }

    companion object {
        fun request(
            id: UUID,
            unitId: UUID,
            publicationId: UUID,
            guestUserId: UUID,
            fromDate: LocalDate,
            toDate: LocalDate,
            occupants: Int,
            message: String?,
            pricePerNight: BigDecimal,
            currency: String,
            cancellationPolicy: CancellationPolicyType?,
            now: Instant,
        ): Reservation {
            if (toDate <= fromDate) {
                throw InvalidArgumentException("toDate must be after fromDate")
            }
            if (ChronoUnit.DAYS.between(fromDate, toDate) > 366) {
                throw InvalidArgumentException("Stay too long (max 366 nights)")
            }
            if (occupants <= 0) {
                throw InvalidArgumentException("occupants must be positive")
            }
            if (pricePerNight.signum() <= 0) {
                throw InvalidArgumentException("pricePerNight must be positive")
            }
            if (fromDate.isBefore(LocalDate.now())) {
                throw InvalidArgumentException("fromDate must not be in the past")
            }
            return Reservation(
                id = id,
                unitId = unitId,
                publicationId = publicationId,
                guestUserId = guestUserId,
                fromDate = fromDate,
                toDate = toDate,
                occupants = occupants,
                message = message?.trim()?.takeIf { it.isNotEmpty() },
                status = ReservationStatus.SOLICITADA,
                pricePerNight = pricePerNight,
                currency = currency,
                cancellationPolicy = cancellationPolicy,
                holdExpiresAt = null,
                createdAt = now,
            )
        }
    }
}
