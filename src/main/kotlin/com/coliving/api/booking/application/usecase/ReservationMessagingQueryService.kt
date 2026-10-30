package com.coliving.api.booking.application.usecase

import com.coliving.api.booking.application.dto.ReservationPartiesInfo
import com.coliving.api.booking.application.port.out.UnitCatalogPort
import com.coliving.api.booking.application.query.ReservationMessagingQuery
import com.coliving.api.booking.domain.repository.ReservationRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReservationMessagingQueryService(
    private val reservationRepository: ReservationRepository,
    private val unitCatalogPort: UnitCatalogPort,
) : ReservationMessagingQuery {

    @Transactional(readOnly = true)
    override fun partiesOf(reservationId: UUID): ReservationPartiesInfo? {
        val reservation = reservationRepository.findById(reservationId) ?: return null
        val room = unitCatalogPort.findBookableUnit(reservation.unitId) ?: return null
        return ReservationPartiesInfo(
            reservationId = reservation.id,
            unitId = reservation.unitId,
            publicationId = reservation.publicationId,
            guestUserId = reservation.guestUserId,
            hostUserId = room.hostId,
        )
    }
}
