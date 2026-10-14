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
 * Pure domain <-> entity mapping, mirroring accommodation's mappers. Persisted
 * rows are hydrated through the internal constructor so stored state is kept
 * exactly (including legacy statuses).
 */
object BookingMappers {

    fun toDomain(entity: ReservationEntity): Reservation = hydrate(entity)

    fun toDomainList(entities: List<ReservationEntity>): List<Reservation> = entities.map { toDomain(it) }

    fun toEntity(reservation: Reservation): ReservationEntity =
        ReservationEntity(
            id = reservation.id,
            unitId = reservation.unitId,
            publicationId = reservation.publicationId,
            guestUserId = reservation.guestUserId,
            places = reservation.places,
            message = reservation.message,
            status = reservation.status,
            monthlyPrice = reservation.monthlyPrice,
            currency = reservation.currency,
            priceUnit = reservation.priceUnit,
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

    private fun hydrate(entity: ReservationEntity): Reservation =
        Reservation(
            id = entity.id,
            unitId = entity.unitId,
            publicationId = entity.publicationId,
            guestUserId = entity.guestUserId,
            places = entity.places,
            message = entity.message,
            status = entity.status,
            monthlyPrice = entity.monthlyPrice,
            currency = entity.currency,
            priceUnit = entity.priceUnit,
            createdAt = entity.createdAt,
        )
}
