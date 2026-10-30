package com.coliving.api.accommodation.infrastructure.persistence.repository

import com.coliving.api.accommodation.domain.enums.PublicationStatus
import com.coliving.api.accommodation.infrastructure.persistence.entity.PropertyEntity
import com.coliving.api.accommodation.infrastructure.persistence.entity.PropertyPhotoEntity
import com.coliving.api.accommodation.infrastructure.persistence.entity.PublicationEntity
import com.coliving.api.accommodation.infrastructure.persistence.entity.UnitEntity
import com.coliving.api.accommodation.infrastructure.persistence.entity.UnitPhotoEntity
import jakarta.persistence.LockModeType
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PropertyJpaRepository : JpaRepository<PropertyEntity, UUID> {
    fun findByHostIdOrderByCreatedAtAsc(hostId: UUID): List<PropertyEntity>
}

interface UnitJpaRepository : JpaRepository<UnitEntity, UUID> {
    fun findByPropertyIdOrderBySortOrderAscCreatedAtAsc(propertyId: UUID): List<UnitEntity>
    fun findByPropertyIdIn(propertyIds: List<UUID>): List<UnitEntity>

    /**
     * Pessimistic write lock used when a room's occupancy is about to change
     * (accepting a request, reducing capacity). Two concurrent operations on the
     * same room serialize here, so the last free place cannot be given twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UnitEntity u WHERE u.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): UnitEntity?

    @Query(
        """
        SELECT u FROM UnitEntity u
        WHERE u.propertyId IN (
            SELECT p.id FROM PropertyEntity p WHERE p.hostId = :hostId
        )
        ORDER BY u.propertyId, u.sortOrder, u.createdAt
        """,
    )
    fun findByHostId(@Param("hostId") hostId: UUID): List<UnitEntity>
}

interface PublicationJpaRepository : JpaRepository<PublicationEntity, UUID> {
    fun findByUnitId(unitId: UUID): PublicationEntity?
    fun findByStatus(status: PublicationStatus): List<PublicationEntity>
    fun findByStatusOrderByCreatedAtDesc(status: PublicationStatus): List<PublicationEntity>
    fun findByUnitIdIn(unitIds: List<UUID>): List<PublicationEntity>
}

interface PropertyPhotoJpaRepository : JpaRepository<PropertyPhotoEntity, UUID> {
    fun findByPropertyIdOrderByPositionAscCreatedAtAsc(propertyId: UUID): List<PropertyPhotoEntity>
}

interface UnitPhotoJpaRepository : JpaRepository<UnitPhotoEntity, UUID> {
    fun findByUnitIdOrderByPositionAscCreatedAtAsc(unitId: UUID): List<UnitPhotoEntity>
}
