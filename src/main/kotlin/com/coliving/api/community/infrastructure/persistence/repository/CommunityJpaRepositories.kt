package com.coliving.api.community.infrastructure.persistence.repository

import com.coliving.api.community.domain.enums.ParticipationStatus
import com.coliving.api.community.infrastructure.persistence.entity.ActivityEntity
import com.coliving.api.community.infrastructure.persistence.entity.ActivityParticipantEntity
import jakarta.persistence.LockModeType
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ActivityJpaRepository : JpaRepository<ActivityEntity, UUID> {

    fun findByPropertyIdOrderByScheduledAtDesc(propertyId: UUID): List<ActivityEntity>

    fun findByPropertyIdInOrderByScheduledAtDesc(propertyIds: List<UUID>): List<ActivityEntity>

    fun findByIdInOrderByScheduledAtDesc(ids: List<UUID>): List<ActivityEntity>

    /**
     * Locking read used by the attendance flow: `SELECT ... FOR UPDATE` on the
     * activity row, so concurrent confirmations serialize and the capacity can
     * never be exceeded (RF-061).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM ActivityEntity a WHERE a.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): ActivityEntity?
}

interface ActivityParticipantJpaRepository : JpaRepository<ActivityParticipantEntity, UUID> {

    fun findByActivityIdOrderByRegisteredAtAsc(activityId: UUID): List<ActivityParticipantEntity>

    fun findByActivityIdAndUserId(activityId: UUID, userId: UUID): ActivityParticipantEntity?

    fun findByUserIdOrderByRegisteredAtDesc(userId: UUID): List<ActivityParticipantEntity>

    fun findByActivityIdAndStatus(
        activityId: UUID,
        status: ParticipationStatus,
    ): List<ActivityParticipantEntity>
}