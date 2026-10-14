package com.coliving.api.booking.application.usecase

import com.coliving.api.booking.application.query.ReservationOccupancyQuery
import com.coliving.api.booking.domain.repository.ReservationRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReservationOccupancyQueryService(
    private val reservationRepository: ReservationRepository,
) : ReservationOccupancyQuery {

    @Transactional(readOnly = true)
    override fun occupiedPlacesOf(unitIds: Collection<UUID>): Map<UUID, Int> =
        reservationRepository.occupiedPlacesOf(unitIds)

    @Transactional(readOnly = true)
    override fun residentIdsOf(unitId: UUID): List<UUID> =
        reservationRepository.residentIdsOf(unitId)
}
