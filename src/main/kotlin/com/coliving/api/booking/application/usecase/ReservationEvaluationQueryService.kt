package com.coliving.api.booking.application.usecase

import com.coliving.api.booking.application.dto.ReservationRelationInfo
import com.coliving.api.booking.application.port.out.UnitCatalogPort
import com.coliving.api.booking.application.query.ReservationEvaluationQuery
import com.coliving.api.booking.domain.repository.ReservationRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReservationEvaluationQueryService(
    private val reservationRepository: ReservationRepository,
    private val unitCatalogPort: UnitCatalogPort,
) : ReservationEvaluationQuery {

    @Transactional(readOnly = true)
    override fun relationOf(reservationId: UUID): ReservationRelationInfo? {
        val reservation = reservationRepository.findById(reservationId) ?: return null
        val hostUserId = unitCatalogPort.hostOfUnit(reservation.unitId) ?: return null
        return ReservationRelationInfo(
            reservationId = reservation.id,
            unitId = reservation.unitId,
            guestUserId = reservation.guestUserId,
            hostUserId = hostUserId,
            status = reservation.status.name,
        )
    }

    @Transactional(readOnly = true)
    override fun confirmedStayCount(userId: UUID): Int {
        val asGuest = reservationRepository.findByGuest(userId).count { it.holdsInventory }
        val hostedUnitIds = unitCatalogPort.unitIdsOfHost(userId)
        if (hostedUnitIds.isEmpty()) return asGuest
        val asHost = reservationRepository.findByUnitIds(hostedUnitIds)
            .count { it.holdsInventory && it.guestUserId != userId }
        return asGuest + asHost
    }
}
