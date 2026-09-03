package com.coliving.api.booking.application.usecase

import com.coliving.api.booking.application.dto.ConfirmReservationCommand
import com.coliving.api.booking.application.dto.ReservationView
import com.coliving.api.booking.application.port.out.UnitAvailabilityPort
import com.coliving.api.booking.domain.enums.ReservationEventType
import com.coliving.api.booking.domain.repository.ReservationRepository
import java.time.Instant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Guest confirms an accepted reservation inside the provisional hold window
 * (RF-043). The held days transition BLOQUEADO -> OCUPADO in accommodation.
 * An expired hold is expired here and then rejected with a conflict.
 */
@Service
class ConfirmReservationService(
    private val reservationRepository: ReservationRepository,
    private val createReservationService: CreateReservationService,
    private val unitAvailabilityPort: UnitAvailabilityPort,
    private val expireStaleHoldsService: ExpireStaleHoldsService,
) {

    @Transactional
    fun confirm(command: ConfirmReservationCommand): ReservationView {
        val now = Instant.now()
        val reservation = createReservationService.loadReservation(command.reservationId)
        createReservationService.requireGuest(reservation, command.guestId)

        // A hold that already elapsed is closed first: the guest then gets a
        // clear conflict instead of silently confirming a stale reservation.
        if (reservation.status == com.coliving.api.booking.domain.enums.ReservationStatus.ACEPTADA) {
            reservation.holdExpiresAt?.takeIf { now.isAfter(it) }?.let {
                expireStaleHoldsService.expireOne(reservation, now)
            }
        }

        reservation.confirm(now)
        unitAvailabilityPort.confirmRange(
            reservation.unitId,
            reservation.fromDate,
            reservation.toDate,
            reservation.id,
        )
        reservationRepository.save(reservation)
        createReservationService.recordEvent(
            reservation.id,
            ReservationEventType.CONFIRMADA,
            command.guestId,
            null,
            now,
        )
        return createReservationService.toView(reservation)
    }
}
