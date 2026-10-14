package com.coliving.api.accommodation.infrastructure.persistence.mapper

import com.coliving.api.accommodation.domain.model.Property
import com.coliving.api.accommodation.domain.model.PropertyPhoto
import com.coliving.api.accommodation.domain.model.Publication
import com.coliving.api.accommodation.domain.model.Unit
import com.coliving.api.accommodation.domain.model.UnitPhoto
import com.coliving.api.accommodation.infrastructure.persistence.entity.PropertyEntity
import com.coliving.api.accommodation.infrastructure.persistence.entity.PropertyPhotoEntity
import com.coliving.api.accommodation.infrastructure.persistence.entity.PublicationEntity
import com.coliving.api.accommodation.infrastructure.persistence.entity.UnitEntity
import com.coliving.api.accommodation.infrastructure.persistence.entity.UnitPhotoEntity
import java.time.Instant
import java.util.UUID

object AccommodationMappers {

    private const val SEPARATOR = ","

    // ---------- Property ----------

    fun toEntity(domain: Property): PropertyEntity =
        PropertyEntity(
            id = domain.id,
            hostId = domain.hostId,
            title = domain.title ?: "",
            description = domain.description,
            cityId = domain.cityId,
            cityName = domain.cityName,
            neighborhood = domain.neighborhood,
            address = domain.address,
            amenities = domain.amenities.joinToString(SEPARATOR),
            archived = domain.archived,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    fun copyInto(entity: PropertyEntity, domain: Property): PropertyEntity {
        entity.hostId = domain.hostId
        entity.title = domain.title ?: ""
        entity.description = domain.description
        entity.cityId = domain.cityId
        entity.cityName = domain.cityName
        entity.neighborhood = domain.neighborhood
        entity.address = domain.address
        entity.amenities = domain.amenities.joinToString(SEPARATOR)
        entity.archived = domain.archived
        entity.updatedAt = Instant.now()
        return entity
    }

    fun toDomainProperty(entity: PropertyEntity): Property =
        Property(
            id = entity.id,
            hostId = entity.hostId,
            title = entity.title,
            description = entity.description,
            cityId = entity.cityId,
            cityName = entity.cityName,
            neighborhood = entity.neighborhood,
            address = entity.address,
            amenities = entity.amenities?.split(SEPARATOR).orEmpty().filter { it.isNotBlank() },
            archived = entity.archived,
        )

    // ---------- Unit (room) ----------

    fun toEntity(domain: Unit): UnitEntity =
        UnitEntity(
            id = domain.id,
            propertyId = domain.propertyId,
            name = domain.name,
            description = domain.description,
            maxGuests = domain.maxGuests,
            monthlyPrice = domain.monthlyPrice,
            currency = domain.currency,
            priceUnit = domain.priceUnit,
            roomType = domain.roomType,
            bathroomType = domain.bathroomType,
            airConditioning = domain.airConditioning,
            fan = domain.fan,
            commonAreas = domain.commonAreas.joinToString(SEPARATOR),
            typeCode = domain.typeCode,
            accessibilityCodes = domain.accessibilityCodes.joinToString(SEPARATOR),
            sortOrder = domain.sortOrder,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    fun copyInto(entity: UnitEntity, domain: Unit): UnitEntity {
        entity.name = domain.name
        entity.description = domain.description
        entity.maxGuests = domain.maxGuests
        entity.monthlyPrice = domain.monthlyPrice
        entity.currency = domain.currency
        entity.priceUnit = domain.priceUnit
        entity.roomType = domain.roomType
        entity.bathroomType = domain.bathroomType
        entity.airConditioning = domain.airConditioning
        entity.fan = domain.fan
        entity.commonAreas = domain.commonAreas.joinToString(SEPARATOR)
        entity.typeCode = domain.typeCode
        entity.accessibilityCodes = domain.accessibilityCodes.joinToString(SEPARATOR)
        entity.sortOrder = domain.sortOrder
        entity.updatedAt = Instant.now()
        return entity
    }

    fun toDomainUnit(entity: UnitEntity): Unit =
        Unit(
            id = entity.id,
            propertyId = entity.propertyId,
            name = entity.name,
            description = entity.description,
            maxGuests = entity.maxGuests,
            monthlyPrice = entity.monthlyPrice,
            currency = entity.currency,
            priceUnit = entity.priceUnit,
            roomType = entity.roomType,
            bathroomType = entity.bathroomType,
            airConditioning = entity.airConditioning,
            fan = entity.fan,
            commonAreas = entity.commonAreas?.split(SEPARATOR).orEmpty().filter { it.isNotBlank() },
            typeCode = entity.typeCode,
            accessibilityCodes = entity.accessibilityCodes?.split(SEPARATOR).orEmpty()
                .filter { it.isNotBlank() }.map { UUID.fromString(it.trim()) }.toSet(),
            sortOrder = entity.sortOrder,
        )

    // ---------- Publication ----------

    fun toEntity(domain: Publication): PublicationEntity =
        PublicationEntity(
            id = domain.id,
            unitId = domain.unitId,
            title = domain.title,
            status = domain.status,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            reviewRequestedAt = domain.reviewRequestedAt,
            reviewDecision = domain.reviewDecision,
            reviewModeratorId = domain.reviewModeratorId,
            reviewNote = domain.reviewNote,
            reviewSourceStatus = domain.reviewSourceStatus,
            services = domain.services.joinToString(SEPARATOR),
            applicableRuleCodes = domain.applicableRuleCodes.joinToString(SEPARATOR),
        )

    fun copyInto(entity: PublicationEntity, domain: Publication): PublicationEntity {
        entity.title = domain.title
        entity.status = domain.status
        entity.updatedAt = domain.updatedAt
        entity.reviewRequestedAt = domain.reviewRequestedAt
        entity.reviewDecision = domain.reviewDecision
        entity.reviewModeratorId = domain.reviewModeratorId
        entity.reviewNote = domain.reviewNote
        entity.reviewSourceStatus = domain.reviewSourceStatus
        entity.services = domain.services.joinToString(SEPARATOR)
        entity.applicableRuleCodes = domain.applicableRuleCodes.joinToString(SEPARATOR)
        return entity
    }

    fun toDomainPublication(entity: PublicationEntity): Publication =
        Publication(
            id = entity.id,
            unitId = entity.unitId,
            title = entity.title,
            status = entity.status,
            createdAt = entity.createdAt,
            reviewRequestedAt = entity.reviewRequestedAt,
            reviewDecision = entity.reviewDecision,
            reviewModeratorId = entity.reviewModeratorId,
            reviewNote = entity.reviewNote,
            reviewSourceStatus = entity.reviewSourceStatus,
            services = entity.services.toUuidSet(),
            applicableRuleCodes = entity.applicableRuleCodes.toUuidSet(),
        )

    private fun String?.toUuidSet(): Set<UUID> =
        this?.split(SEPARATOR).orEmpty()
            .filter { it.isNotBlank() }
            .map { UUID.fromString(it.trim()) }
            .toSet()

    // ---------- Photographs ----------

    fun toDomainPhoto(entity: PropertyPhotoEntity): PropertyPhoto =
        PropertyPhoto(entity.id, entity.propertyId, entity.url, entity.position)

    fun toEntity(photo: PropertyPhoto): PropertyPhotoEntity =
        PropertyPhotoEntity(photo.id, photo.propertyId, photo.url, photo.position, Instant.now())

    fun copyInto(entity: PropertyPhotoEntity, photo: PropertyPhoto): PropertyPhotoEntity {
        entity.url = photo.url
        entity.position = photo.position
        return entity
    }

    fun toDomainPhoto(entity: UnitPhotoEntity): UnitPhoto =
        UnitPhoto(entity.id, entity.unitId, entity.url, entity.position)

    fun toEntity(photo: UnitPhoto): UnitPhotoEntity =
        UnitPhotoEntity(photo.id, photo.unitId, photo.url, photo.position, Instant.now())

    fun copyInto(entity: UnitPhotoEntity, photo: UnitPhoto): UnitPhotoEntity {
        entity.url = photo.url
        entity.position = photo.position
        return entity
    }
}
