package com.coliving.api.booking.application.usecase

import com.coliving.api.booking.application.dto.ReservationEventView
import com.coliving.api.booking.application.dto.ReservationView
import com.coliving.api.booking.application.port.out.UnitCatalogPort
import com.coliving.api.booking.domain.repository.ReservationEventRepository
import com.coliving.api.booking.domain.repository.ReservationRepository
import com.coliving.api.shared.error.ForbiddenException
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read side: a guest sees their own reservations; a host sees the reservations
 * arriving for the units they own. History (RF-042) is exposed per reservation.
 */
@Service
class ListReservationsService(
    private val reservationRepository: ReservationRepository,
    private val reservationEventRepository: ReservationEventRepository,
    private val unitCatalogPort: UnitCatalogPort,
    private val createReservationService: CreateReservationService,
) {

    @Transactional(readOnly = true)
    fun listMine(guestId: UUID): List<ReservationView> =
        reservationRepository.findByGuest(guestId).map { createReservationService.toView(it) }

    @Transactional(readOnly = true)
    fun listForHost(hostId: UUID): List<ReservationView> {
        val unitIds = unitCatalogPort.unitIdsOfHost(hostId)
        if (unitIds.isEmpty()) return emptyList()
        return reservationRepository.findByUnitIds(unitIds).map { createReservationService.toView(it) }
    }

    @Transactional(readOnly = true)
    fun history(reservationId: UUID, requesterId: UUID): List<ReservationEventView> {
        val reservation = reservationRepository.findById(reservationId)
            ?: throw com.coliving.api.shared.error.NotFoundException("Reservation not found")
        val allowed = reservation.guestUserId == requesterId ||
            unitCatalogPort.isHostOfUnit(reservation.unitId, requesterId)
        if (!allowed) throw ForbiddenException("Not a party of this reservation")
        return reservationEventRepository.findByReservation(reservationId).map {
            ReservationEventView(
                id = it.id,
                reservationId = it.reservationId,
                type = it.type,
                actorUserId = it.actorUserId,
                reason = it.reason,
                occurredAt = it.occurredAt,
            )
        }
    }
}
