package com.coliving.api.accommodation.domain.repository

import com.coliving.api.accommodation.domain.model.AvailabilitySlot
import com.coliving.api.accommodation.domain.model.Pricing
import com.coliving.api.accommodation.domain.model.Property
import com.coliving.api.accommodation.domain.model.Publication
import com.coliving.api.accommodation.domain.model.Rule
import com.coliving.api.accommodation.domain.model.Unit
import java.time.LocalDate
import java.util.UUID

interface PropertyRepository {
    fun findById(id: UUID): Property?
    fun findByHost(hostId: UUID): List<Property>
    fun save(property: Property)
}

interface UnitRepository {
    fun findById(id: UUID): Unit?
    fun findByProperty(propertyId: UUID): List<Unit>
    fun save(unit: Unit)
}

interface PublicationRepository {
    fun findById(id: UUID): Publication?
    fun findByUnit(unitId: UUID): Publication?
    fun findByStatus(status: com.coliving.api.accommodation.domain.enums.PublicationStatus): List<Publication>
    fun findAllPublished(): List<Publication>
    fun save(publication: Publication)
}

interface PricingRepository {
    fun findByPublication(publicationId: UUID): Pricing?
    fun save(pricing: Pricing)
}

interface RuleRepository {
    fun findByPublication(publicationId: UUID): Rule?
    fun save(rule: Rule)
}

/**
 * Availability persistence port. Implementations must promise the locking
 * discipline (single transaction + SELECT ... FOR UPDATE in ascending date
 * order) so concurrent reservations cannot double-book.
 */
interface AvailabilitySlotRepository {

    /** Returns the existing rows for the range, or creates+returns implicit free rows. */
    fun findOrCreateRange(unitId: UUID, from: LocalDate, to: LocalDate): List<AvailabilitySlot>

    /** Returns the rows for a range that belong to the given reservation/host block. */
    fun findRange(unitId: UUID, from: LocalDate, to: LocalDate): List<AvailabilitySlot>

    /** Reads all days in the range (including implicit free days) for read-only queries. */
    fun findRangeReadOnly(unitId: UUID, from: LocalDate, to: LocalDate): Map<LocalDate, AvailabilitySlot>

    fun isRangeAvailable(unitId: UUID, from: LocalDate, to: LocalDate): Boolean

    fun saveAll(slots: List<AvailabilitySlot>)
    fun deleteRange(unitId: UUID, from: LocalDate, to: LocalDate)
}