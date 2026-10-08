package com.coliving.api.booking.application.usecase

import com.coliving.api.booking.application.dto.HostDecisionCommand
import com.coliving.api.booking.application.dto.ReservationView
import com.coliving.api.booking.application.port.out.UnitCatalogPort
import com.coliving.api.booking.domain.enums.ReservationEventType
import com.coliving.api.booking.domain.repository.ReservationRepository
import com.coliving.api.shared.error.ConflictException
import java.time.Instant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Host accepts a monthly request. The room row is locked for the rest of the
 * transaction before checking the remaining places, so two concurrent
 * acceptances can never take the same last place.
 */
@Service
class AcceptReservationService(
    private val reservationRepository: ReservationRepository,
    private val createReservationService: CreateReservationService,
    private val unitCatalogPort: UnitCatalogPort,
) {

    @Transactional
    fun accept(command: HostDecisionCommand): ReservationView {
        val now = Instant.now()
        val reservation = createReservationService.loadReservation(command.reservationId)
        createReservationService.requireHostOfUnit(reservation.unitId, command.hostId)

        // Pessimistic lock: serializes every acceptance for this room.
        val room = unitCatalogPort.lockBookableUnit(reservation.unitId)
            ?: throw ConflictException("La habitación ya no existe")
        if (room.status != "PUBLICADA") {
            throw ConflictException("La habitación está pausada; no puede aceptar solicitudes")
        }
        if (room.monthlyPrice == null) {
            throw ConflictException("La habitación no tiene precio mensual configurado")
        }
        val occupied = reservationRepository.occupiedPlacesOf(listOf(room.unitId))[room.unitId] ?: 0
        val available = (room.capacity - occupied).coerceAtLeast(0)
        if (available < reservation.places) {
            throw ConflictException("La habitación ya no tiene plazas disponibles")
        }

        reservation.accept(now)
        reservationRepository.save(reservation)
        createReservationService.recordEvent(
            reservation.id,
            ReservationEventType.ACEPTADA,
            command.hostId,
            command.reason,
            now,
        )
        return createReservationService.toView(reservation, room)
    }
}
