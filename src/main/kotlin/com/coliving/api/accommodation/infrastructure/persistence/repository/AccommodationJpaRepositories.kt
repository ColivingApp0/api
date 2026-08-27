package com.coliving.api.accommodation.infrastructure.persistence.repository

import com.coliving.api.accommodation.domain.enums.PublicationStatus
import com.coliving.api.accommodation.infrastructure.persistence.entity.AvailabilitySlotEntity
import com.coliving.api.accommodation.infrastructure.persistence.entity.PricingEntity
import com.coliving.api.accommodation.infrastructure.persistence.entity.PropertyEntity
import com.coliving.api.accommodation.infrastructure.persistence.entity.PublicationEntity
import com.coliving.api.accommodation.infrastructure.persistence.entity.RuleEntity
import com.coliving.api.accommodation.infrastructure.persistence.entity.UnitEntity
import jakarta.persistence.LockModeType
import java.time.LocalDate
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PropertyJpaRepository : JpaRepository<PropertyEntity, UUID> {
    fun findByHostIdOrderByCreatedAtAsc(hostId: UUID): List<PropertyEntity>
}

interface UnitJpaRepository : JpaRepository<UnitEntity, UUID> {
    fun findByPropertyId(propertyId: UUID): List<UnitEntity>
}

interface PublicationJpaRepository : JpaRepository<PublicationEntity, UUID> {
    fun findByUnitId(unitId: UUID): PublicationEntity?
    fun findByStatus(status: PublicationStatus): List<PublicationEntity>
    fun findByStatusOrderByCreatedAtDesc(status: PublicationStatus): List<PublicationEntity>
}

interface PricingJpaRepository : JpaRepository<PricingEntity, UUID> {
    fun findByPublicationId(publicationId: UUID): PricingEntity?
}

interface RuleJpaRepository : JpaRepository<RuleEntity, UUID> {
    fun findByPublicationId(publicationId: UUID): RuleEntity?
}

interface AvailabilitySlotJpaRepository : JpaRepository<AvailabilitySlotEntity, Long> {

    /**
     * Row-materializing upsert: inserts every missing day as DISPONIBLE.
     * Without this, the implicit available days would have no rows to lock.
     */
    @Modifying
    @Query(
        value = """
        INSERT INTO accommodation_availability_slot (unit_id, date, state, reservation_id)
        SELECT :unitId, day, 'DISPONIBLE', NULL
        FROM generate_series(:from, :to, INTERVAL '1 day') AS day
        ON CONFLICT (unit_id, date) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertMissingDays(@Param("unitId") unitId: UUID, @Param("from") from: LocalDate, @Param("to") to: LocalDate)

    /**
     * Pessimistic lock of every day in the range, ascending. This is the
     * anti-double-booking guarantee: two transactions locking the same days
     * serialize here, and the second one observes the first's BLOQUEADO state.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AvailabilitySlotEntity s WHERE s.unitId = :unitId AND s.date >= :from AND s.date < :to ORDER BY s.date ASC")
    fun findRangeLocked(@Param("unitId") unitId: UUID, @Param("from") from: LocalDate, @Param("to") to: LocalDate): List<AvailabilitySlotEntity>

    @Query("SELECT s FROM AvailabilitySlotEntity s WHERE s.unitId = :unitId AND s.date >= :from AND s.date < :to ORDER BY s.date ASC")
    fun findRange(@Param("unitId") unitId: UUID, @Param("from") from: LocalDate, @Param("to") to: LocalDate): List<AvailabilitySlotEntity>

    /**
     * Read-only availability query: whether any day in the range is not free.
     * Missing days are free by definition.
     */
    @Query(
        "SELECT COUNT(s) FROM AvailabilitySlotEntity s WHERE s.unitId = :unitId AND s.date >= :from AND s.date < :to AND s.state <> 'DISPONIBLE'",
    )
    fun countNotAvailable(@Param("unitId") unitId: UUID, @Param("from") from: LocalDate, @Param("to") to: LocalDate): Long

    @Modifying
    @Query(
        value = """
        DELETE FROM accommodation_availability_slot
        WHERE unit_id = :unitId AND date >= :from AND date < :to AND state = 'DISPONIBLE' AND reservation_id IS NULL
        """,
        nativeQuery = true,
    )
    fun deleteCleanSlots(@Param("unitId") unitId: UUID, @Param("from") from: LocalDate, @Param("to") to: LocalDate)
}