package com.coliving.api.booking.infrastructure.persistence.repository

import com.coliving.api.booking.domain.enums.ReservationStatus
import com.coliving.api.booking.infrastructure.persistence.entity.ReservationEntity
import com.coliving.api.booking.infrastructure.persistence.entity.ReservationEventEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReservationJpaRepository : JpaRepository<ReservationEntity, UUID> {
    fun findByGuestUserIdOrderByCreatedAtDesc(guestUserId: UUID): List<ReservationEntity>

    fun findByUnitIdInOrderByCreatedAtDesc(unitIds: List<UUID>): List<ReservationEntity>

    @Query(
        """
        SELECT COALESCE(SUM(r.places), 0)
        FROM ReservationEntity r
        WHERE r.unitId = :unitId AND r.status IN :statuses
        """,
    )
    fun occupiedPlaces(
        @Param("unitId") unitId: UUID,
        @Param("statuses") statuses: List<ReservationStatus>,
    ): Long

    @Query(
        """
        SELECT r.unitId, COALESCE(SUM(r.places), 0)
        FROM ReservationEntity r
        WHERE r.unitId IN :unitIds AND r.status IN :statuses
        GROUP BY r.unitId
        """,
    )
    fun occupiedPlacesByUnit(
        @Param("unitIds") unitIds: Collection<UUID>,
        @Param("statuses") statuses: List<ReservationStatus>,
    ): List<Array<Any>>

    @Query(
        """
        SELECT DISTINCT r.guestUserId
        FROM ReservationEntity r
        WHERE r.unitId = :unitId AND r.status IN :statuses
        """,
    )
    fun residentIds(
        @Param("unitId") unitId: UUID,
        @Param("statuses") statuses: List<ReservationStatus>,
    ): List<UUID>

    fun existsByGuestUserIdAndUnitIdAndStatusIn(
        guestUserId: UUID,
        unitId: UUID,
        statuses: Collection<ReservationStatus>,
    ): Boolean
}

interface ReservationEventJpaRepository : JpaRepository<ReservationEventEntity, UUID> {
    fun findByReservationIdOrderByOccurredAtAsc(reservationId: UUID): List<ReservationEventEntity>
}
