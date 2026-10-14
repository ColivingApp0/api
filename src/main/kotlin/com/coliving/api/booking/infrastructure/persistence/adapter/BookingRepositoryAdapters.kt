package com.coliving.api.booking.infrastructure.persistence.adapter

import com.coliving.api.booking.domain.model.Reservation
import com.coliving.api.booking.domain.model.ReservationEvent
import com.coliving.api.booking.domain.repository.ACTIVE_STATUSES
import com.coliving.api.booking.domain.repository.OCCUPYING_STATUSES
import com.coliving.api.booking.domain.repository.ReservationEventRepository
import com.coliving.api.booking.domain.repository.ReservationRepository
import com.coliving.api.booking.infrastructure.persistence.mapper.BookingMappers
import com.coliving.api.booking.infrastructure.persistence.repository.ReservationEventJpaRepository
import com.coliving.api.booking.infrastructure.persistence.repository.ReservationJpaRepository
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class ReservationRepositoryAdapter(
    private val jpaRepository: ReservationJpaRepository,
) : ReservationRepository {

    override fun findById(id: UUID): Reservation? =
        jpaRepository.findById(id).map { BookingMappers.toDomain(it) }.orElse(null)

    @Transactional(propagation = Propagation.MANDATORY)
    override fun save(reservation: Reservation) {
        jpaRepository.save(BookingMappers.toEntity(reservation))
    }

    override fun findByGuest(guestUserId: UUID): List<Reservation> =
        BookingMappers.toDomainList(jpaRepository.findByGuestUserIdOrderByCreatedAtDesc(guestUserId))

    override fun findByUnitIds(unitIds: List<UUID>): List<Reservation> =
        if (unitIds.isEmpty()) {
            emptyList()
        } else {
            BookingMappers.toDomainList(jpaRepository.findByUnitIdInOrderByCreatedAtDesc(unitIds))
        }

    override fun occupiedPlacesOf(unitIds: Collection<UUID>): Map<UUID, Int> {
        if (unitIds.isEmpty()) return emptyMap()
        return jpaRepository.occupiedPlacesByUnit(unitIds, OCCUPYING_STATUSES)
            .associate { row -> (row[0] as UUID) to (row[1] as Number).toInt() }
    }

    override fun residentIdsOf(unitId: UUID): List<UUID> =
        jpaRepository.residentIds(unitId, OCCUPYING_STATUSES)

    override fun hasActiveRequest(guestUserId: UUID, unitId: UUID): Boolean =
        jpaRepository.existsByGuestUserIdAndUnitIdAndStatusIn(guestUserId, unitId, ACTIVE_STATUSES)
}

@Component
class ReservationEventRepositoryAdapter(
    private val jpaRepository: ReservationEventJpaRepository,
) : ReservationEventRepository {

    override fun findByReservation(reservationId: UUID): List<ReservationEvent> =
        jpaRepository.findByReservationIdOrderByOccurredAtAsc(reservationId).map(BookingMappers::toEventDomain)

    @Transactional(propagation = Propagation.MANDATORY)
    override fun save(event: ReservationEvent) {
        jpaRepository.save(BookingMappers.toEventEntity(event))
    }
}
