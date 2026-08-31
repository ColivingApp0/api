package com.coliving.api.booking.application.usecase

import com.coliving.api.booking.application.dto.ReservationView
import com.coliving.api.booking.application.port.out.UnitAvailabilityPort
import com.coliving.api.booking.application.port.out.UnitCatalogPort
import com.coliving.api.booking.domain.enums.ReservationEventType
import com.coliving.api.booking.domain.model.Reservation
import com.coliving.api.booking.domain.repository.ReservationEventRepository
import com.coliving.api.booking.domain.repository.ReservationRepository
import java.time.Instant
import java.util.UUID
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Releases provisional holds whose window elapsed without guest confirmation
 * (RF-043: "lo libera automáticamente si no se completa el siguiente paso").
 * Runs lazily on every sweep and can also be triggered inline (confirm).
 */
@Service
class ExpireStaleHoldsService(
    private val reservationRepository: ReservationRepository,
    private val reservationEventRepository: ReservationEventRepository,
    private val unitAvailabilityPort: UnitAvailabilityPort,
) {

    /** Runs every minute; annotations honored because the app enables scheduling. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    fun sweep() {
        expireStale(Instant.now())
    }

    @Transactional
    fun expireStale(now: Instant): Int {
        val stale = reservationRepository.findExpiredHolds(now)
        stale.forEach { expireOne(it, now) }
        return stale.size
    }

    @Transactional
    fun expireOne(reservation: Reservation, now: Instant) {
        reservation.expire(now)
        unitAvailabilityPort.unlockRange(
            reservation.unitId,
            reservation.fromDate,
            reservation.toDate,
            reservation.id,
        )
        reservationRepository.save(reservation)
        reservationEventRepository.save(
            com.coliving.api.booking.domain.model.ReservationEvent(
                id = UUID.randomUUID(),
                reservationId = reservation.id,
                type = ReservationEventType.EXPIRADA,
                actorUserId = reservation.guestUserId,
                reason = "Provisional hold window elapsed",
                occurredAt = now,
            ),
        )
    }
}
