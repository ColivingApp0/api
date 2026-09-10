package com.coliving.api.booking.application.usecase

import com.coliving.api.booking.application.query.ReservationCommunityQuery
import com.coliving.api.booking.domain.enums.ReservationStatus
import com.coliving.api.booking.domain.repository.ReservationRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read-side implementation of [ReservationCommunityQuery]: a confirmed
 * reservation is what makes a guest a resident of the property. Pure projection
 * over reservations — no mutation, and the residency rule itself belongs to the
 * community context's requirements.
 */
@Service
class ReservationCommunityQueryService(
    private val reservationRepository: ReservationRepository,
) : ReservationCommunityQuery {

    @Transactional(readOnly = true)
    override fun confirmedGuestIds(unitIds: List<UUID>): List<UUID> {
        if (unitIds.isEmpty()) return emptyList()
        return reservationRepository.findByUnitIds(unitIds)
            .filter { it.status == ReservationStatus.CONFIRMADA }
            .map { it.guestUserId }
            .distinct()
    }
}