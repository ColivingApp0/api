package com.coliving.api.accommodation.infrastructure.persistence.adapter

import com.coliving.api.accommodation.domain.model.PropertyImage
import com.coliving.api.accommodation.domain.repository.PropertyImageRepository
import com.coliving.api.accommodation.infrastructure.persistence.entity.PropertyImageEntity
import com.coliving.api.accommodation.infrastructure.persistence.repository.PropertyImageJpaRepository
import java.util.UUID
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class PropertyImageRepositoryAdapter(
    private val jpa: PropertyImageJpaRepository,
) : PropertyImageRepository {

    @Transactional(readOnly = true)
    override fun findById(id: UUID): PropertyImage? =
        jpa.findById(id).orElse(null)?.toDomain()

    @Transactional(readOnly = true)
    override fun findByProperty(propertyId: UUID): List<PropertyImage> =
        jpa.findByPropertyIdOrderByPositionAscCreatedAtAscIdAsc(propertyId).map { it.toDomain() }

    @Transactional
    override fun save(image: PropertyImage) {
        jpa.save(image.toEntity())
    }

    @Transactional
    override fun delete(image: PropertyImage) {
        jpa.findById(image.id).orElse(null)?.let(jpa::delete)
    }

    private fun PropertyImage.toEntity(): PropertyImageEntity =
        PropertyImageEntity(
            id = id,
            propertyId = propertyId,
            storageKey = storageKey,
            originalFilename = originalFilename,
            contentType = contentType,
            sizeBytes = sizeBytes,
            position = position,
            createdAt = createdAt,
        )

    private fun PropertyImageEntity.toDomain(): PropertyImage =
        PropertyImage(
            id = id,
            propertyId = propertyId,
            storageKey = storageKey,
            originalFilename = originalFilename,
            contentType = contentType,
            sizeBytes = sizeBytes,
            position = position,
            createdAt = createdAt,
        )
}
