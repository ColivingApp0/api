package com.coliving.api.booking.application.usecase

import com.coliving.api.booking.application.dto.CancelReservationCommand
import com.coliving.api.booking.application.dto.ReservationView
import com.coliving.api.booking.application.port.out.UnitAvailabilityPort
import com.coliving.api.booking.domain.enums.ReservationEventType
import com.coliving.api.booking.domain.model.Reservation
import com.coliving.api.booking.domain.repository.ReservationRepository
import com.coliving.api.shared.error.InvalidArgumentException
import java.time.Instant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Guest or host cancels with a mandatory reason (RF-044). If the reservation
 * holds inventory (ACEPTADA/CONFIRMADA), the unit's days are released back to
 * available in the same transaction. The event records actor and reason so
 * reputation can later consume cancellations.
 */
@Service
class CancelReservationService(
    private val reservationRepository: ReservationRepository,
    private val createReservationService: CreateReservationService,
    private val unitAvailabilityPort: UnitAvailabilityPort,
) {

    @Transactional
    fun cancel(command: CancelReservationCommand): ReservationView {
        if (command.reason.isBlank()) {
            throw InvalidArgumentException("A cancellation reason is required")
        }
        val now = Instant.now()
        val reservation = createReservationService.loadReservation(command.reservationId)

        val isGuest = reservation.guestUserId == command.actorId
        val isHost = !isGuest && unitCatalogIsHost(reservation, command.actorId)
        if (!isGuest && !isHost) {
            throw com.coliving.api.shared.error.ForbiddenException(
                "Only the guest or the host can cancel this reservation",
            )
        }

        val released = reservation.holdsInventory
        reservation.cancel(now)
        if (released) {
            unitAvailabilityPort.unlockRange(
                reservation.unitId,
                reservation.fromDate,
                reservation.toDate,
                reservation.id,
            )
        }
        reservationRepository.save(reservation)
        createReservationService.recordEvent(
            reservation.id,
            ReservationEventType.CANCELADA,
            command.actorId,
            command.reason,
            now,
        )
        return createReservationService.toView(reservation)
    }

    private fun unitCatalogIsHost(reservation: Reservation, actorId: java.util.UUID): Boolean =
        createReservationService.unitCatalogPort.isHostOfUnit(reservation.unitId, actorId)
}
