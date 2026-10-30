package com.coliving.api.booking.application.usecase

import com.coliving.api.booking.application.dto.CancelReservationCommand
import com.coliving.api.booking.application.dto.ReservationView
import com.coliving.api.booking.domain.enums.ReservationEventType
import com.coliving.api.booking.domain.repository.ReservationRepository
import com.coliving.api.shared.error.InvalidArgumentException
import java.time.Instant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Guest or host cancels a request with a mandatory reason. Cancelling an
 * accepted request frees its places automatically because occupancy is derived
 * from accepted reservations, never from a stored counter.
 */
@Service
class CancelReservationService(
    private val reservationRepository: ReservationRepository,
    private val createReservationService: CreateReservationService,
) {

    @Transactional
    fun cancel(command: CancelReservationCommand): ReservationView {
        if (command.reason.isBlank()) {
            throw InvalidArgumentException("Indica el motivo de la cancelación")
        }
        val now = Instant.now()
        val reservation = createReservationService.loadReservation(command.reservationId)
        val isGuest = reservation.guestUserId == command.actorId
        val isHost = !isGuest && createReservationService.unitCatalogPort.isHostOfUnit(
            reservation.unitId,
            command.actorId,
        )
        if (!isGuest && !isHost) {
            throw com.coliving.api.shared.error.ForbiddenException(
                "Solo el huésped o el anfitrión pueden cancelar esta solicitud",
            )
        }
        reservation.cancel(now)
        reservationRepository.save(reservation)
        createReservationService.recordEvent(
            reservation.id,
            ReservationEventType.CANCELADA,
            command.actorId,
            command.reason,
            now,
        )
        return createReservationService.toView(
            reservation,
            createReservationService.unitCatalogPort.findBookableUnit(reservation.unitId),
        )
    }
}
