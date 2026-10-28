package com.coliving.api.accommodation.infrastructure.persistence.repository

import com.coliving.api.accommodation.infrastructure.persistence.entity.PropertyImageEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface PropertyImageJpaRepository : JpaRepository<PropertyImageEntity, UUID> {
    fun findByPropertyIdOrderByPositionAscCreatedAtAscIdAsc(propertyId: UUID): List<PropertyImageEntity>
}
