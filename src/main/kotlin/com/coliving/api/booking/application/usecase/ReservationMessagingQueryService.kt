package com.coliving.api.booking.application.usecase

import com.coliving.api.booking.application.dto.ReservationPartiesInfo
import com.coliving.api.booking.application.port.out.UnitCatalogPort
import com.coliving.api.booking.application.query.ReservationMessagingQuery
import com.coliving.api.booking.domain.repository.ReservationRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read-side implementation of [ReservationMessagingQuery]: pure projection over
 * reservations plus the unit owner read from accommodation. No mutation and no
 * business decision — deciding who may open a conversation is messaging's job.
 */
@Service
class ReservationMessagingQueryService(
    private val reservationRepository: ReservationRepository,
    private val unitCatalogPort: UnitCatalogPort,
) : ReservationMessagingQuery {

    @Transactional(readOnly = true)
    override fun partiesOf(reservationId: UUID): ReservationPartiesInfo? {
        val reservation = reservationRepository.findById(reservationId) ?: return null
        val hostUserId = unitCatalogPort.hostOfUnit(reservation.unitId) ?: return null
        return ReservationPartiesInfo(
            reservationId = reservation.id,
            guestUserId = reservation.guestUserId,
            hostUserId = hostUserId,
            fromDate = reservation.fromDate,
            toDate = reservation.toDate,
        )
    }
}