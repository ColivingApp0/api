package com.coliving.api.accommodation.application

import com.coliving.api.accommodation.domain.enums.AvailabilityState
import com.coliving.api.accommodation.domain.model.AvailabilitySlot
import com.coliving.api.accommodation.domain.model.Property
import com.coliving.api.accommodation.domain.model.Unit
import com.coliving.api.accommodation.domain.repository.AvailabilitySlotRepository
import com.coliving.api.accommodation.domain.repository.PropertyRepository
import com.coliving.api.accommodation.domain.repository.UnitRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

class FakePropertyRepository : PropertyRepository {
    val store = mutableMapOf<UUID, Property>()

    override fun findById(id: UUID): Property? = store[id]

    override fun findByHost(hostId: UUID): List<Property> = store.values.filter { it.hostId == hostId }

    override fun save(property: Property) {
        store[property.id] = property
    }
}

class FakeUnitRepository : UnitRepository {
    val store = mutableMapOf<UUID, Unit>()

    override fun findById(id: UUID): Unit? = store[id]

    override fun findByProperty(propertyId: UUID): List<Unit> = store.values.filter { it.propertyId == propertyId }

    override fun save(unit: Unit) {
        store[unit.id] = unit
    }
}

/**
 * In-memory availability store. `findOrCreateRange` returns a new list with
 * implicit days filled; a single synthetic "current state" read is used so
 * callers mutate the returned slots and `saveAll` persists them. Concurrency
 * guarantees are exercised by AvailabilityConcurrencyIT against real Postgres.
 */
class FakeAvailabilitySlotRepository : AvailabilitySlotRepository {
    private val store = mutableMapOf<Pair<UUID, LocalDate>, AvailabilitySlot>()

    override fun findOrCreateRange(unitId: UUID, from: LocalDate, to: LocalDate): List<AvailabilitySlot> {
        return dates(from, to).map { date ->
            store.getOrPut(unitId to date) { AvailabilitySlot.of(unitId, date) }
        }
    }

    override fun findRange(unitId: UUID, from: LocalDate, to: LocalDate): List<AvailabilitySlot> =
        dates(from, to).mapNotNull { store[unitId to it] }

    override fun findRangeReadOnly(unitId: UUID, from: LocalDate, to: LocalDate): Map<LocalDate, AvailabilitySlot> =
        dates(from, to).associateWith { date -> store[unitId to date] ?: AvailabilitySlot.of(unitId, date) }

    override fun isRangeAvailable(unitId: UUID, from: LocalDate, to: LocalDate): Boolean =
        dates(from, to).all { date -> (store[unitId to date]?.state ?: AvailabilityState.DISPONIBLE) == AvailabilityState.DISPONIBLE }

    override fun saveAll(slots: List<AvailabilitySlot>) {
        slots.forEach { store[it.unitId to it.date] = it }
    }

    override fun deleteRange(unitId: UUID, from: LocalDate, to: LocalDate) {
        dates(from, to).forEach { date ->
            val slot = store[unitId to date]
            if (slot != null && slot.state == AvailabilityState.DISPONIBLE && slot.reservationId == null) {
                store.remove(unitId to date)
            }
        }
    }

    private fun dates(from: LocalDate, to: LocalDate): List<LocalDate> {
        val nights = ChronoUnit.DAYS.between(from, to)
        return (0 until nights).map { from.plusDays(it) }
    }
}