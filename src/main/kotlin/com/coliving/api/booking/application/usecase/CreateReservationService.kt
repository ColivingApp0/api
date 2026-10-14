package com.coliving.api.booking.application.usecase

import com.coliving.api.booking.application.dto.CreateReservationCommand
import com.coliving.api.booking.application.dto.ReservationView
import com.coliving.api.booking.application.port.out.BookableUnit
import com.coliving.api.booking.application.port.out.GuestVerificationPort
import com.coliving.api.booking.application.port.out.UnitCatalogPort
import com.coliving.api.booking.domain.enums.ReservationEventType
import com.coliving.api.booking.domain.model.Reservation
import com.coliving.api.booking.domain.model.ReservationEvent
import com.coliving.api.booking.domain.repository.ReservationEventRepository
import com.coliving.api.booking.domain.repository.ReservationRepository
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates a monthly room request (RF-040 adapted).
 *
 * Guards, in order:
 * 1. Room exists and its publication is PUBLICADA (RN-03).
 * 2. Monthly price is configured and the room has free places.
 * 3. Guest profile meets the minimum verification level.
 * 4. The guest has no other active request for the same room.
 *
 * A pending request does NOT occupy a place; only acceptance does.
 */
@Service
class CreateReservationService(
    private val reservationRepository: ReservationRepository,
    private val reservationEventRepository: ReservationEventRepository,
    internal val unitCatalogPort: UnitCatalogPort,
    private val guestVerificationPort: GuestVerificationPort,
) {

    @Transactional
    fun create(command: CreateReservationCommand): ReservationView {
        val now = Instant.now()
        val room = unitCatalogPort.findBookableUnit(command.unitId)
            ?: throw NotFoundException("Habitación no encontrada")
        if (room.status != "PUBLICADA") {
            throw ConflictException("La habitación no acepta solicitudes en este momento")
        }
        val monthlyPrice = room.monthlyPrice
            ?: throw ConflictException("La habitación no tiene precio mensual configurado")
        if (command.places <= 0) {
            throw ConflictException("Debes solicitar al menos una plaza")
        }
        val occupied = reservationRepository.occupiedPlacesOf(listOf(room.unitId))[room.unitId] ?: 0
        val available = (room.capacity - occupied).coerceAtLeast(0)
        if (available <= 0 || command.places > available) {
            throw ConflictException("La habitación no tiene plazas disponibles")
        }
        if (reservationRepository.hasActiveRequest(command.guestId, room.unitId)) {
            throw ConflictException("Ya tienes una solicitud activa para esta habitación")
        }
        if (!guestVerificationPort.hasMinimumVerification(command.guestId)) {
            throw ForbiddenException("Completa tu verificación para enviar solicitudes")
        }

        val reservation = Reservation.request(
            id = UUID.randomUUID(),
            unitId = room.unitId,
            publicationId = room.publicationId,
            guestUserId = command.guestId,
            places = command.places,
            message = command.message,
            monthlyPrice = monthlyPrice,
            currency = room.currency.name,
            priceUnit = room.priceUnit.name,
            now = now,
        )
        reservationRepository.save(reservation)
        recordEvent(reservation.id, ReservationEventType.CREADA, command.guestId, null, now)
        return toView(reservation, room)
    }

    internal fun recordEvent(
        reservationId: UUID,
        type: ReservationEventType,
        actorId: UUID,
        reason: String?,
        now: Instant,
    ) {
        reservationEventRepository.save(
            ReservationEvent(
                id = UUID.randomUUID(),
                reservationId = reservationId,
                type = type,
                actorUserId = actorId,
                reason = reason,
                occurredAt = now,
            ),
        )
    }

    internal fun loadReservation(reservationId: UUID): Reservation =
        reservationRepository.findById(reservationId)
            ?: throw NotFoundException("Solicitud no encontrada")

    internal fun requireHostOfUnit(unitId: UUID, hostId: UUID) {
        if (!unitCatalogPort.isHostOfUnit(unitId, hostId)) {
            throw ForbiddenException("No eres el anfitrión de esta habitación")
        }
    }

    internal fun toView(reservation: Reservation, room: BookableUnit? = null): ReservationView =
        ReservationView(
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
            unitName = room?.unitName,
            propertyTitle = room?.propertyTitle,
            cityName = room?.cityName,
            photoUrl = room?.photoUrl,
            createdAt = reservation.createdAt,
        )
}
