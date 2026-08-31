package com.coliving.api.booking.application.usecase

import com.coliving.api.booking.application.dto.HostDecisionCommand
import com.coliving.api.booking.application.dto.ReservationView
import com.coliving.api.booking.domain.enums.ReservationEventType
import com.coliving.api.booking.domain.repository.ReservationRepository
import java.time.Instant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Host asks the guest for additional information before deciding (RF-041).
 * The request stays alive: the host can later accept or reject it.
 */
@Service
class RequestInfoReservationService(
    private val reservationRepository: ReservationRepository,
    private val createReservationService: CreateReservationService,
) {

    @Transactional
    fun requestInfo(command: HostDecisionCommand): ReservationView {
        val now = Instant.now()
        val reservation = createReservationService.loadReservation(command.reservationId)
        createReservationService.requireHostOfUnit(reservation.unitId, command.hostId)

        reservation.requestInfo(now)
        reservationRepository.save(reservation)
        createReservationService.recordEvent(
            reservation.id,
            ReservationEventType.INFORMACION_SOLICITADA,
            command.hostId,
            command.reason,
            now,
        )
        return createReservationService.toView(reservation)
    }
}
