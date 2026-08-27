package com.coliving.api.accommodation.infrastructure.persistence.mapper

import com.coliving.api.accommodation.domain.model.AvailabilitySlot
import com.coliving.api.accommodation.domain.model.Pricing
import com.coliving.api.accommodation.domain.model.Property
import com.coliving.api.accommodation.domain.model.Publication
import com.coliving.api.accommodation.domain.model.Rule
import com.coliving.api.accommodation.domain.model.Unit
import com.coliving.api.accommodation.infrastructure.persistence.entity.AvailabilitySlotEntity
import com.coliving.api.accommodation.infrastructure.persistence.entity.PricingEntity
import com.coliving.api.accommodation.infrastructure.persistence.entity.PropertyEntity
import com.coliving.api.accommodation.infrastructure.persistence.entity.PublicationEntity
import com.coliving.api.accommodation.infrastructure.persistence.entity.RuleEntity
import com.coliving.api.accommodation.infrastructure.persistence.entity.UnitEntity
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
        entity.address = domain.address
        entity.amenities = domain.amenities.joinToString(SEPARATOR)
        entity.archived = domain.archived
        entity.updatedAt = Instant.now()
        return entity
    }

    fun toProperty(domain: Property): Property = Property(
        id = domain.id,
        hostId = domain.hostId,
        title = domain.title ?: "",
        description = domain.description,
        cityId = domain.cityId,
        address = domain.address,
        amenities = domain.amenities.toList(),
        archived = domain.archived,
    )

    fun toDomainProperty(entity: PropertyEntity): Property =
        Property(
            id = entity.id,
            hostId = entity.hostId,
            title = entity.title,
            description = entity.description,
            cityId = entity.cityId,
            address = entity.address,
            amenities = entity.amenities?.split(SEPARATOR).orEmpty(),
            archived = entity.archived,
        )

    // ---------- Unit ----------

    fun toEntity(domain: Unit): UnitEntity =
        UnitEntity(
            id = domain.id,
            propertyId = domain.propertyId,
            name = domain.name,
            maxGuests = domain.maxGuests,
            bedrooms = domain.bedrooms,
            beds = domain.beds,
            bathrooms = domain.bathrooms,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    fun copyInto(entity: UnitEntity, domain: Unit): UnitEntity {
        entity.name = domain.name
        entity.maxGuests = domain.maxGuests
        entity.bedrooms = domain.bedrooms
        entity.beds = domain.beds
        entity.bathrooms = domain.bathrooms
        entity.updatedAt = Instant.now()
        return entity
    }

    fun toDomainUnit(entity: UnitEntity): Unit =
        Unit(
            id = entity.id,
            propertyId = entity.propertyId,
            name = entity.name,
            maxGuests = entity.maxGuests,
            bedrooms = entity.bedrooms,
            beds = entity.beds,
            bathrooms = entity.bathrooms,
        )

    // ---------- Publication ----------

    fun toEntity(domain: Publication): PublicationEntity =
        PublicationEntity(
            id = domain.id,
            unitId = domain.unitId,
            title = domain.title,
            status = domain.status,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    fun copyInto(entity: PublicationEntity, domain: Publication): PublicationEntity {
        entity.title = domain.title
        entity.status = domain.status
        entity.updatedAt = Instant.now()
        return entity
    }

    fun toDomainPublication(entity: PublicationEntity): Publication =
        Publication(
            id = entity.id,
            unitId = entity.unitId,
            title = entity.title,
            status = entity.status,
        )

    // ---------- Pricing ----------

    fun toEntity(domain: Pricing): PricingEntity =
        PricingEntity(
            id = domain.id,
            publicationId = domain.publicationId,
            basePricePerNight = domain.basePricePerNight,
            currency = domain.currency,
            updatedAt = Instant.now(),
        )

    fun copyInto(entity: PricingEntity, domain: Pricing): PricingEntity {
        entity.basePricePerNight = domain.basePricePerNight
        entity.currency = domain.currency
        entity.updatedAt = Instant.now()
        return entity
    }

    fun toDomainPricing(entity: PricingEntity): Pricing =
        Pricing(
            id = entity.id,
            publicationId = entity.publicationId,
            basePricePerNight = entity.basePricePerNight,
            currency = entity.currency,
        )

    // ---------- Rule ----------

    fun toEntity(domain: Rule): RuleEntity =
        RuleEntity(
            id = domain.id,
            publicationId = domain.publicationId,
            cancellationPolicy = domain.cancellationPolicy,
            checkInTime = domain.checkInTime,
            checkOutTime = domain.checkOutTime,
            minNights = domain.minNights,
            maxNights = domain.maxNights,
            houseRules = domain.houseRules.joinToString(SEPARATOR),
            updatedAt = Instant.now(),
        )

    fun copyInto(entity: RuleEntity, domain: Rule): RuleEntity {
        entity.cancellationPolicy = domain.cancellationPolicy
        entity.checkInTime = domain.checkInTime
        entity.checkOutTime = domain.checkOutTime
        entity.minNights = domain.minNights
        entity.maxNights = domain.maxNights
        entity.houseRules = domain.houseRules.joinToString(SEPARATOR)
        entity.updatedAt = Instant.now()
        return entity
    }

    fun toDomainRule(entity: RuleEntity): Rule =
        Rule(
            id = entity.id,
            publicationId = entity.publicationId,
            cancellationPolicy = entity.cancellationPolicy,
            checkInTime = entity.checkInTime,
            checkOutTime = entity.checkOutTime,
            minNights = entity.minNights,
            maxNights = entity.maxNights,
            houseRules = entity.houseRules?.split(SEPARATOR).orEmpty(),
        )

    // ---------- AvailabilitySlot ----------

    fun toEntity(domain: AvailabilitySlot): AvailabilitySlotEntity =
        AvailabilitySlotEntity(
            id = domain.dbId ?: 0L,
            unitId = domain.unitId,
            date = domain.date,
            state = domain.state,
            reservationId = domain.reservationId,
        )

    fun toDomainSlot(entity: AvailabilitySlotEntity): AvailabilitySlot =
        AvailabilitySlot.of(
            unitId = entity.unitId,
            date = entity.date,
            state = entity.state,
            reservationId = entity.reservationId,
            dbId = entity.id,
        )
}