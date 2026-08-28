package com.coliving.api.accommodation.infrastructure.persistence.adapter

import com.coliving.api.accommodation.domain.model.AvailabilitySlot
import com.coliving.api.accommodation.domain.repository.AvailabilitySlotRepository
import com.coliving.api.accommodation.infrastructure.persistence.mapper.AccommodationMappers.toDomainSlot
import com.coliving.api.accommodation.infrastructure.persistence.mapper.AccommodationMappers.toEntity
import com.coliving.api.accommodation.infrastructure.persistence.repository.AvailabilitySlotJpaRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * Implementation of the locking discipline:
 * 1. `insertMissingDays` materializes every implicit free day (ON CONFLICT no-op).
 * 2. `findRangeLocked` takes a `SELECT ... FOR UPDATE` over the range, ascending.
 */
@Repository
class AvailabilitySlotRepositoryAdapter(
    private val jpa: AvailabilitySlotJpaRepository,
) : AvailabilitySlotRepository {

    @Transactional
    override fun findOrCreateRange(unitId: UUID, from: LocalDate, to: LocalDate): List<AvailabilitySlot> {
        jpa.insertMissingDays(unitId, from, to)
        return jpa.findRangeLocked(unitId, from, to).map(::toDomainSlot)
    }

    @Transactional
    override fun findRange(unitId: UUID, from: LocalDate, to: LocalDate): List<AvailabilitySlot> =
        jpa.findRangeLocked(unitId, from, to).map(::toDomainSlot)

    @Transactional(readOnly = true)
    override fun findRangeReadOnly(unitId: UUID, from: LocalDate, to: LocalDate): Map<LocalDate, AvailabilitySlot> {
        val rows = jpa.findRange(unitId, from, to).associateBy { it.date to it.unitId }
        val nights = ChronoUnit.DAYS.between(from, to)
        return (0 until nights).associate { offset ->
            val date = from.plusDays(offset)
            date to (rows[date to unitId]?.let(::toDomainSlot) ?: AvailabilitySlot.of(unitId, date))
        }
    }

    @Transactional(readOnly = true)
    override fun isRangeAvailable(unitId: UUID, from: LocalDate, to: LocalDate): Boolean =
        jpa.countNotAvailable(unitId, from, to) == 0L

    @Transactional
    override fun saveAll(slots: List<AvailabilitySlot>) {
        jpa.saveAll(slots.map(::toEntity))
    }

    @Transactional
    override fun deleteRange(unitId: UUID, from: LocalDate, to: LocalDate) {
        jpa.deleteCleanSlots(unitId, from, to)
    }
}