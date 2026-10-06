package com.coliving.api.accommodation.infrastructure.persistence.adapter

import com.coliving.api.accommodation.domain.enums.PublicationStatus
import com.coliving.api.accommodation.domain.model.Property
import com.coliving.api.accommodation.domain.model.PropertyPhoto
import com.coliving.api.accommodation.domain.model.Publication
import com.coliving.api.accommodation.domain.model.Unit
import com.coliving.api.accommodation.domain.model.UnitPhoto
import com.coliving.api.accommodation.domain.repository.PropertyPhotoRepository
import com.coliving.api.accommodation.domain.repository.PropertyRepository
import com.coliving.api.accommodation.domain.repository.PublicationRepository
import com.coliving.api.accommodation.domain.repository.UnitPhotoRepository
import com.coliving.api.accommodation.domain.repository.UnitRepository
import com.coliving.api.accommodation.infrastructure.persistence.mapper.AccommodationMappers.copyInto
import com.coliving.api.accommodation.infrastructure.persistence.mapper.AccommodationMappers.toDomainPhoto
import com.coliving.api.accommodation.infrastructure.persistence.mapper.AccommodationMappers.toDomainProperty
import com.coliving.api.accommodation.infrastructure.persistence.mapper.AccommodationMappers.toDomainPublication
import com.coliving.api.accommodation.infrastructure.persistence.mapper.AccommodationMappers.toDomainUnit
import com.coliving.api.accommodation.infrastructure.persistence.mapper.AccommodationMappers.toEntity
import com.coliving.api.accommodation.infrastructure.persistence.repository.PropertyJpaRepository
import com.coliving.api.accommodation.infrastructure.persistence.repository.PropertyPhotoJpaRepository
import com.coliving.api.accommodation.infrastructure.persistence.repository.PublicationJpaRepository
import com.coliving.api.accommodation.infrastructure.persistence.repository.UnitJpaRepository
import com.coliving.api.accommodation.infrastructure.persistence.repository.UnitPhotoJpaRepository
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

    @Transactional
    override fun findByIdForUpdate(id: UUID): Unit? =
        jpa.findByIdForUpdate(id)?.let(::toDomainUnit)

    @Transactional(readOnly = true)
    override fun findByProperty(propertyId: UUID): List<Unit> =
        jpa.findByPropertyIdOrderBySortOrderAscCreatedAtAsc(propertyId).map(::toDomainUnit)

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
class PropertyPhotoRepositoryAdapter(
    private val jpa: PropertyPhotoJpaRepository,
) : PropertyPhotoRepository {

    @Transactional(readOnly = true)
    override fun findByProperty(propertyId: UUID): List<PropertyPhoto> =
        jpa.findByPropertyIdOrderByPositionAscCreatedAtAsc(propertyId).map(::toDomainPhoto)

    @Transactional(readOnly = true)
    override fun findById(id: UUID): PropertyPhoto? =
        jpa.findById(id).orElse(null)?.let(::toDomainPhoto)

    @Transactional
    override fun save(photo: PropertyPhoto) {
        val existing = jpa.findById(photo.id).orElse(null)
        if (existing != null) {
            copyInto(existing, photo)
        } else {
            jpa.save(toEntity(photo))
        }
    }

    @Transactional
    override fun delete(photo: PropertyPhoto) {
        jpa.deleteById(photo.id)
    }
}

@Repository
class UnitPhotoRepositoryAdapter(
    private val jpa: UnitPhotoJpaRepository,
) : UnitPhotoRepository {

    @Transactional(readOnly = true)
    override fun findByUnit(unitId: UUID): List<UnitPhoto> =
        jpa.findByUnitIdOrderByPositionAscCreatedAtAsc(unitId).map(::toDomainPhoto)

    @Transactional(readOnly = true)
    override fun findById(id: UUID): UnitPhoto? =
        jpa.findById(id).orElse(null)?.let(::toDomainPhoto)

    @Transactional
    override fun save(photo: UnitPhoto) {
        val existing = jpa.findById(photo.id).orElse(null)
        if (existing != null) {
            copyInto(existing, photo)
        } else {
            jpa.save(toEntity(photo))
        }
    }

    @Transactional
    override fun delete(photo: UnitPhoto) {
        jpa.deleteById(photo.id)
    }
}
