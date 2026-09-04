package com.coliving.api.booking.application.usecase

import com.coliving.api.booking.application.dto.HostDecisionCommand
import com.coliving.api.booking.application.dto.ReservationView
import com.coliving.api.booking.application.port.out.UnitAvailabilityPort
import com.coliving.api.booking.domain.enums.ReservationEventType
import com.coliving.api.booking.domain.repository.ReservationRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Host accepts a request (RF-041). Acceptance provisionally reserves the
 * inventory (RF-043): the unit's days are atomically locked in the same
 * transaction and the reservation gets a hold window (booking.hold-hours,
 * default 24) inside which the guest must confirm, or the hold expires.
 */
@Service
class AcceptReservationService(
    private val reservationRepository: ReservationRepository,
    private val createReservationService: CreateReservationService,
    private val unitAvailabilityPort: UnitAvailabilityPort,
    @Value("\${booking.hold-hours:24}") private val holdHours: Long,
) {

    @Transactional
    fun accept(command: HostDecisionCommand): ReservationView {
        val now = Instant.now()
        val reservation = createReservationService.loadReservation(command.reservationId)
        createReservationService.requireHostOfUnit(reservation.unitId, command.hostId)

        reservation.accept(
            holdExpiresAt = now.plus(holdHours, ChronoUnit.HOURS),
            now = now,
        )
        // Enforcing lock — the no-double-booking invariant lives here.
        unitAvailabilityPort.lockRange(
            reservation.unitId,
            reservation.fromDate,
            reservation.toDate,
            reservation.id,
        )
        reservationRepository.save(reservation)
        createReservationService.recordEvent(
            reservation.id,
            ReservationEventType.ACEPTADA,
            command.hostId,
            command.reason,
            now,
        )
        return createReservationService.toView(reservation)
    }
}
