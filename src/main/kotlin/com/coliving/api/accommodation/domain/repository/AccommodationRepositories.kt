package com.coliving.api.accommodation.domain.repository

import com.coliving.api.accommodation.domain.model.Property
import com.coliving.api.accommodation.domain.model.PropertyPhoto
import com.coliving.api.accommodation.domain.model.Publication
import com.coliving.api.accommodation.domain.model.Unit
import com.coliving.api.accommodation.domain.model.UnitPhoto
import java.util.UUID

interface PropertyRepository {
    fun findById(id: UUID): Property?
    fun findByHost(hostId: UUID): List<Property>
    fun save(property: Property)
}

interface UnitRepository {
    fun findById(id: UUID): Unit?

    /** Row lock used by capacity/occupancy mutations (accept request). */
    fun findByIdForUpdate(id: UUID): Unit?
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

interface PropertyPhotoRepository {
    fun findByProperty(propertyId: UUID): List<PropertyPhoto>
    fun findById(id: UUID): PropertyPhoto?
    fun save(photo: PropertyPhoto)
    fun delete(photo: PropertyPhoto)
}

interface UnitPhotoRepository {
    fun findByUnit(unitId: UUID): List<UnitPhoto>
    fun findById(id: UUID): UnitPhoto?
    fun save(photo: UnitPhoto)
    fun delete(photo: UnitPhoto)
}
