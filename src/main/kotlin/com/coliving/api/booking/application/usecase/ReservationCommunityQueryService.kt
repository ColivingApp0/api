package com.coliving.api.booking.application.usecase

import com.coliving.api.booking.application.query.ReservationCommunityQuery
import com.coliving.api.booking.domain.repository.ReservationRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read-side implementation of [ReservationCommunityQuery]: an accepted monthly
 * request makes the guest a resident of the property (legacy CONFIRMADA rows
 * count as accepted too).
 */
@Service
class ReservationCommunityQueryService(
    private val reservationRepository: ReservationRepository,
) : ReservationCommunityQuery {

    @Transactional(readOnly = true)
    override fun confirmedGuestIds(unitIds: List<UUID>): List<UUID> {
        if (unitIds.isEmpty()) return emptyList()
        return reservationRepository.findByUnitIds(unitIds)
            .filter { it.holdsInventory }
            .map { it.guestUserId }
            .distinct()
    }
}
