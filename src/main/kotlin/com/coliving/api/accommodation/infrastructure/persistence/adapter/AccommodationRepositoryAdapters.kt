package com.coliving.api.accommodation.infrastructure.persistence.adapter

import com.coliving.api.accommodation.domain.enums.PublicationStatus
import com.coliving.api.accommodation.domain.model.Pricing
import com.coliving.api.accommodation.domain.model.Property
import com.coliving.api.accommodation.domain.model.Publication
import com.coliving.api.accommodation.domain.model.Rule
import com.coliving.api.accommodation.domain.model.Unit
import com.coliving.api.accommodation.domain.repository.PricingRepository
import com.coliving.api.accommodation.domain.repository.PropertyRepository
import com.coliving.api.accommodation.domain.repository.PublicationRepository
import com.coliving.api.accommodation.domain.repository.RuleRepository
import com.coliving.api.accommodation.domain.repository.UnitRepository
import com.coliving.api.accommodation.infrastructure.persistence.mapper.AccommodationMappers.copyInto
import com.coliving.api.accommodation.infrastructure.persistence.mapper.AccommodationMappers.toDomainProperty
import com.coliving.api.accommodation.infrastructure.persistence.mapper.AccommodationMappers.toDomainPublication
import com.coliving.api.accommodation.infrastructure.persistence.mapper.AccommodationMappers.toDomainRule
import com.coliving.api.accommodation.infrastructure.persistence.mapper.AccommodationMappers.toDomainUnit
import com.coliving.api.accommodation.infrastructure.persistence.mapper.AccommodationMappers.toEntity
import com.coliving.api.accommodation.infrastructure.persistence.repository.PropertyJpaRepository
import com.coliving.api.accommodation.infrastructure.persistence.repository.PublicationJpaRepository
import com.coliving.api.accommodation.infrastructure.persistence.repository.PricingJpaRepository
import com.coliving.api.accommodation.infrastructure.persistence.repository.RuleJpaRepository
import com.coliving.api.accommodation.infrastructure.persistence.repository.UnitJpaRepository
import java.util.UUID
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class PropertyRepositoryAdapter(
    private val jpa: PropertyJpaRepository,
) : PropertyRepository {

    @Transactional(readOnly = true)
    override fun findById(id: UUID): Property? =
        jpa.findById(id).orElse(null)?.let(::toDomainProperty)

    @Transactional(readOnly = true)
    override fun findByHost(hostId: UUID): List<Property> =
        jpa.findByHostIdOrderByCreatedAtAsc(hostId).map(::toDomainProperty)

    @Transactional
    override fun save(property: Property) {
        val existing = jpa.findById(property.id).orElse(null)
        if (existing != null) {
            copyInto(existing, property)
        } else {
            jpa.save(toEntity(property))
        }
    }
}

@Repository
class UnitRepositoryAdapter(
    private val jpa: UnitJpaRepository,
) : UnitRepository {

    @Transactional(readOnly = true)
    override fun findById(id: UUID): Unit? =
        jpa.findById(id).orElse(null)?.let(::toDomainUnit)

    @Transactional(readOnly = true)
    override fun findByProperty(propertyId: UUID): List<Unit> =
        jpa.findByPropertyId(propertyId).map(::toDomainUnit)

    @Transactional
    override fun save(unit: Unit) {
        val existing = jpa.findById(unit.id).orElse(null)
        if (existing != null) {
            copyInto(existing, unit)
        } else {
            jpa.save(toEntity(unit))
        }
    }
}

@Repository
class PublicationRepositoryAdapter(
    private val jpa: PublicationJpaRepository,
) : PublicationRepository {

    @Transactional(readOnly = true)
    override fun findById(id: UUID): Publication? =
        jpa.findById(id).orElse(null)?.let(::toDomainPublication)

    @Transactional(readOnly = true)
    override fun findByUnit(unitId: UUID): Publication? =
        jpa.findByUnitId(unitId)?.let(::toDomainPublication)

    @Transactional(readOnly = true)
    override fun findByStatus(status: PublicationStatus): List<Publication> =
        jpa.findByStatus(status).map(::toDomainPublication)

    @Transactional(readOnly = true)
    override fun findAllPublished(): List<Publication> =
        jpa.findByStatusOrderByCreatedAtDesc(PublicationStatus.PUBLICADA).map(::toDomainPublication)

    @Transactional
    override fun save(publication: Publication) {
        val existing = jpa.findById(publication.id).orElse(null)
        if (existing != null) {
            copyInto(existing, publication)
        } else {
            jpa.save(toEntity(publication))
        }
    }
}

@Repository
class PricingRepositoryAdapter(
    private val jpa: PricingJpaRepository,
) : PricingRepository {

    @Transactional(readOnly = true)
    override fun findByPublication(publicationId: UUID): Pricing? =
        jpa.findByPublicationId(publicationId)?.let { m ->
            Pricing(
                id = m.id,
                publicationId = m.publicationId,
                basePricePerNight = m.basePricePerNight,
                currency = m.currency,
            )
        }

    @Transactional
    override fun save(pricing: Pricing) {
        val existing = jpa.findById(pricing.id).orElse(null)
        if (existing != null) {
            copyInto(existing, pricing)
        } else {
            jpa.save(toEntity(pricing))
        }
    }
}

@Repository
class RuleRepositoryAdapter(
    private val jpa: RuleJpaRepository,
) : RuleRepository {

    @Transactional(readOnly = true)
    override fun findByPublication(publicationId: UUID): Rule? =
        jpa.findByPublicationId(publicationId)?.let(::toDomainRule)

    @Transactional
    override fun save(rule: Rule) {
        val existing = jpa.findById(rule.id).orElse(null)
        if (existing != null) {
            copyInto(existing, rule)
        } else {
            jpa.save(toEntity(rule))
        }
    }
}