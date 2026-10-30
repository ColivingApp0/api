package com.coliving.api.booking.domain.model

import com.coliving.api.booking.domain.enums.ReservationStatus
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.InvalidArgumentException
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Monthly room-rental request (RF-040..RF-044 adapted to monthly rent).
 *
 * A request references a room and a guest by plain UUID. The monthly price and
 * billing unit are frozen at creation (RN-07). Accepting a request immediately
 * makes it occupy the requested place(s); there are no dates, holds or nights.
 */
class Reservation internal constructor(
    val id: UUID,
    val unitId: UUID,
    val publicationId: UUID,
    val guestUserId: UUID,
    places: Int,
    message: String?,
    status: ReservationStatus,
    val monthlyPrice: BigDecimal?,
    val currency: String,
    val priceUnit: String?,
    val createdAt: Instant,
) {

    var places: Int = places
        private set

    var message: String? = message
        private set

    var status: ReservationStatus = status
        private set

    var updatedAt: Instant = createdAt
        private set

    /** Host accepts: the requested place becomes occupied immediately. */
    fun accept(now: Instant) {
        requireStatus("aceptar", ReservationStatus.SOLICITADA, ReservationStatus.INFORMACION_SOLICITADA)
        status = ReservationStatus.ACEPTADA
        touch(now)
    }

    /** Host rejects: terminal, no place is consumed. */
    fun reject(now: Instant) {
        requireStatus("rechazar", ReservationStatus.SOLICITADA, ReservationStatus.INFORMACION_SOLICITADA)
        status = ReservationStatus.RECHAZADA
        touch(now)
    }

    /** Host asks the guest for more information before deciding. */
    fun requestInfo(now: Instant) {
        requireStatus("solicitar información", ReservationStatus.SOLICITADA)
        status = ReservationStatus.INFORMACION_SOLICITADA
        touch(now)
    }

    /** Guest or host cancels with a mandatory reason. */
    fun cancel(now: Instant) {
        requireStatus(
            "cancelar",
            ReservationStatus.SOLICITADA,
            ReservationStatus.INFORMACION_SOLICITADA,
            ReservationStatus.ACEPTADA,
            ReservationStatus.CONFIRMADA,
        )
        status = ReservationStatus.CANCELADA
        touch(now)
    }

    /** Whether this request currently occupies places of the room. */
    val holdsInventory: Boolean
        get() = status == ReservationStatus.ACEPTADA || status == ReservationStatus.CONFIRMADA

    private fun requireStatus(action: String, vararg allowed: ReservationStatus) {
        if (status !in allowed) {
            throw ConflictException(
                "No se puede $action una solicitud en estado $status (permitidos: ${allowed.joinToString()})",
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
            places: Int,
            message: String?,
            monthlyPrice: BigDecimal,
            currency: String,
            priceUnit: String,
            now: Instant,
        ): Reservation {
            if (places <= 0) {
                throw InvalidArgumentException("Debes solicitar al menos una plaza")
            }
            if (monthlyPrice.signum() <= 0) {
                throw InvalidArgumentException("El precio mensual debe ser positivo")
            }
            return Reservation(
                id = id,
                unitId = unitId,
                publicationId = publicationId,
                guestUserId = guestUserId,
                places = places,
                message = message?.trim()?.takeIf { it.isNotEmpty() },
                status = ReservationStatus.SOLICITADA,
                monthlyPrice = monthlyPrice,
                currency = currency,
                priceUnit = priceUnit,
                createdAt = now,
            )
        }
    }
}
