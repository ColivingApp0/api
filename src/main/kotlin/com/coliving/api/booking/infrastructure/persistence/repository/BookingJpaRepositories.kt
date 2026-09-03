package com.coliving.api.booking.infrastructure.persistence.repository

import com.coliving.api.booking.domain.enums.ReservationStatus
import com.coliving.api.booking.infrastructure.persistence.entity.ReservationEntity
import com.coliving.api.booking.infrastructure.persistence.entity.ReservationEventEntity
import java.time.Instant
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReservationJpaRepository : JpaRepository<ReservationEntity, UUID> {
    fun findByGuestUserIdOrderByCreatedAtDesc(guestUserId: UUID): List<ReservationEntity>

    fun findByUnitIdInOrderByCreatedAtDesc(unitIds: List<UUID>): List<ReservationEntity>

    @Query("SELECT r FROM ReservationEntity r WHERE r.status = :status AND r.holdExpiresAt < :now")
    fun findByStatusAndHoldExpiresAtBefore(
        @Param("status") status: ReservationStatus,
        @Param("now") now: Instant,
    ): List<ReservationEntity>
}

interface ReservationEventJpaRepository : JpaRepository<ReservationEventEntity, UUID> {
    fun findByReservationIdOrderByOccurredAtAsc(reservationId: UUID): List<ReservationEventEntity>
}
