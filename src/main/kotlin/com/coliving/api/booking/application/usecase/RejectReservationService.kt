package com.coliving.api.booking.application.usecase

import com.coliving.api.booking.application.dto.HostDecisionCommand
import com.coliving.api.booking.application.dto.ReservationView
import com.coliving.api.booking.domain.enums.ReservationEventType
import com.coliving.api.booking.domain.repository.ReservationRepository
import java.time.Instant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Host rejects a request (RF-041). The decision notifies the guest (event with
 * optional reason) and moves the reservation to its terminal RECHAZADA state.
 */
@Service
class RejectReservationService(
    private val reservationRepository: ReservationRepository,
    private val createReservationService: CreateReservationService,
) {

    @Transactional
    fun reject(command: HostDecisionCommand): ReservationView {
        val now = Instant.now()
        val reservation = createReservationService.loadReservation(command.reservationId)
        createReservationService.requireHostOfUnit(reservation.unitId, command.hostId)

        reservation.reject(now)
        reservationRepository.save(reservation)
        createReservationService.recordEvent(
            reservation.id,
            ReservationEventType.RECHAZADA,
            command.hostId,
            command.reason,
            now,
        )
        return createReservationService.toView(reservation)
    }
}
