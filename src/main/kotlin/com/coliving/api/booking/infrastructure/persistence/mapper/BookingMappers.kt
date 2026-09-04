package com.coliving.api.booking.infrastructure.persistence.mapper

import com.coliving.api.booking.domain.enums.ReservationEventType
import com.coliving.api.booking.domain.enums.ReservationStatus
import com.coliving.api.booking.domain.model.Reservation
import com.coliving.api.booking.domain.model.ReservationEvent
import com.coliving.api.booking.infrastructure.persistence.entity.ReservationEntity
import com.coliving.api.booking.infrastructure.persistence.entity.ReservationEventEntity
import java.time.Instant
import java.util.UUID

/**
 * Pure domain <-> entity mapping, mirroring accommodation's mappers: the
 * domain model is reconstructed through its state-machine entry point
 * (hydration constructor) so no invariant can be bypassed.
 */
object BookingMappers {

    fun toDomain(entity: ReservationEntity): Reservation {
        val reservation = hydrate(entity)
        // Hold expiry and lifecycle status come straight from storage.
        return reservation
    }

    fun toDomainList(entities: List<ReservationEntity>): List<Reservation> = entities.map { toDomain(it) }

    fun toEntity(reservation: Reservation): ReservationEntity =
        ReservationEntity(
            id = reservation.id,
            unitId = reservation.unitId,
            publicationId = reservation.publicationId,
            guestUserId = reservation.guestUserId,
            fromDate = reservation.fromDate,
            toDate = reservation.toDate,
            occupants = reservation.occupants,
            message = reservation.message,
            status = reservation.status,
            pricePerNight = reservation.pricePerNight,
            currency = reservation.currency,
            cancellationPolicy = reservation.cancellationPolicy,
            holdExpiresAt = reservation.holdExpiresAt,
            createdAt = reservation.createdAt,
            updatedAt = reservation.updatedAt,
        )

    fun toEventDomain(entity: ReservationEventEntity): ReservationEvent =
        ReservationEvent(
            id = entity.id,
            reservationId = entity.reservationId,
            type = entity.eventType,
            actorUserId = entity.actorUserId,
            reason = entity.reason,
            occurredAt = entity.occurredAt,
        )

    fun toEventEntity(event: ReservationEvent): ReservationEventEntity =
        ReservationEventEntity(
            id = event.id,
            reservationId = event.reservationId,
            eventType = event.type,
            actorUserId = event.actorUserId,
            reason = event.reason,
            occurredAt = event.occurredAt,
        )

    /**
     * Recreates a persisted reservation in exactly the stored state. Uses the
     * internal constructor so persisted state skips creation validation (e.g.
     * dates that were valid when created).
     */
    private fun hydrate(entity: ReservationEntity): Reservation =
        ReservationInternalFactory.create(
            id = entity.id,
            unitId = entity.unitId,
            publicationId = entity.publicationId,
            guestUserId = entity.guestUserId,
            fromDate = entity.fromDate,
            toDate = entity.toDate,
            occupants = entity.occupants,
            message = entity.message,
            status = entity.status,
            pricePerNight = entity.pricePerNight,
            currency = entity.currency,
            cancellationPolicy = entity.cancellationPolicy,
            holdExpiresAt = entity.holdExpiresAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
}

/**
 * Hydration entry point. Lives outside Reservation to keep the aggregate's
 * public API free of persistence concerns while still reaching the internal
 * constructor.
 */
object ReservationInternalFactory {
    fun create(
        id: UUID,
        unitId: UUID,
        publicationId: UUID,
        guestUserId: UUID,
        fromDate: java.time.LocalDate,
        toDate: java.time.LocalDate,
        occupants: Int,
        message: String?,
        status: ReservationStatus,
        pricePerNight: java.math.BigDecimal,
        currency: String,
        cancellationPolicy: com.coliving.api.booking.domain.enums.CancellationPolicyType?,
        holdExpiresAt: Instant?,
        createdAt: Instant,
        updatedAt: Instant,
    ): Reservation {
        val reservation = Reservation(
            id, unitId, publicationId, guestUserId, fromDate, toDate,
            occupants, message, status, pricePerNight, currency,
            cancellationPolicy, holdExpiresAt, createdAt,
        )
        return reservation
    }
}
