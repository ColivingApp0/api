package com.coliving.api.accommodation.domain.repository

import com.coliving.api.accommodation.domain.model.PropertyImage
import java.util.UUID

interface PropertyImageRepository {
    fun findById(id: UUID): PropertyImage?
    fun findByProperty(propertyId: UUID): List<PropertyImage>
    fun save(image: PropertyImage)
    fun delete(image: PropertyImage)
}
