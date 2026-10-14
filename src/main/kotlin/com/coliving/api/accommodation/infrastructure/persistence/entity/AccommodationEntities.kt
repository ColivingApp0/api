package com.coliving.api.accommodation.infrastructure.persistence.entity

import com.coliving.api.accommodation.domain.enums.BathroomType
import com.coliving.api.accommodation.domain.enums.Currency
import com.coliving.api.accommodation.domain.enums.PriceUnit
import com.coliving.api.accommodation.domain.enums.PublicationStatus
import com.coliving.api.accommodation.domain.enums.RoomType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "accommodation_property")
class PropertyEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "host_id", nullable = false)
    var hostId: UUID,

    @Column(name = "title", nullable = false, length = 255)
    var title: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String?,

    @Column(name = "city_id")
    var cityId: UUID?,

    @Column(name = "city_name", length = 120)
    var cityName: String?,

    @Column(name = "neighborhood", length = 120)
    var neighborhood: String?,

    @Column(name = "address", length = 255)
    var address: String?,

    @Column(name = "amenities", columnDefinition = "TEXT")
    var amenities: String?,

    @Column(name = "archived", nullable = false)
    var archived: Boolean,

    @Column(name = "created_at", nullable = false)
    var createdAt: java.time.Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: java.time.Instant,
) {
    @jakarta.persistence.PreUpdate
    fun onUpdate() {
        updatedAt = java.time.Instant.now()
    }
}

@Entity
@Table(name = "accommodation_unit")
class UnitEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "property_id", nullable = false)
    var propertyId: UUID,

    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String?,

    @Column(name = "max_guests", nullable = false)
    var maxGuests: Int,

    @Column(name = "monthly_price", precision = 12, scale = 2)
    var monthlyPrice: BigDecimal?,

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 5)
    var currency: Currency,

    @Enumerated(EnumType.STRING)
    @Column(name = "price_unit", nullable = false, length = 20)
    var priceUnit: PriceUnit,

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    var roomType: RoomType,

    @Enumerated(EnumType.STRING)
    @Column(name = "bathroom_type", nullable = false, length = 20)
    var bathroomType: BathroomType,

    @Column(name = "air_conditioning", nullable = false)
    var airConditioning: Boolean,

    @Column(name = "fan", nullable = false)
    var fan: Boolean,

    @Column(name = "common_areas", columnDefinition = "TEXT")
    var commonAreas: String?,

    // Kept for schema compatibility with the original unit model. Rooms do not
    // use bedrooms/beds/bathrooms as a business rule anymore.
    @Column(name = "bedrooms", nullable = false)
    var bedrooms: Int = 0,

    @Column(name = "beds", nullable = false)
    var beds: Int = 1,

    @Column(name = "bathrooms", nullable = false)
    var bathrooms: Int = 1,

    // Catalog references (RF-031, RF-083): plain UUIDs, no FK.
    @Column(name = "type_code")
    var typeCode: UUID?,

    @Column(name = "accessibility_codes", columnDefinition = "TEXT")
    var accessibilityCodes: String?,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: java.time.Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: java.time.Instant,
)

@Entity
@Table(name = "accommodation_publication")
class PublicationEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "unit_id", nullable = false)
    var unitId: UUID,

    @Column(name = "title", nullable = false, length = 255)
    var title: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    var status: PublicationStatus,

    @Column(name = "created_at", nullable = false)
    var createdAt: java.time.Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: java.time.Instant,

    // Moderation review metadata (RF-022 "enviar a revisión", RF-081).
    @Column(name = "review_requested_at")
    var reviewRequestedAt: java.time.Instant?,

    @Enumerated(EnumType.STRING)
    @Column(name = "review_decision", length = 40)
    var reviewDecision: com.coliving.api.accommodation.domain.enums.ReviewDecision?,

    @Column(name = "review_moderator_id")
    var reviewModeratorId: UUID?,

    @Column(name = "review_note", columnDefinition = "TEXT")
    var reviewNote: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "review_source_status", length = 50)
    var reviewSourceStatus: PublicationStatus?,

    // Searchable catalog references (RF-031): csv of catalog entry UUIDs.
    @Column(name = "services", columnDefinition = "TEXT")
    var services: String?,

    @Column(name = "applicable_rule_codes", columnDefinition = "TEXT")
    var applicableRuleCodes: String?,
)

@Entity
@Table(name = "accommodation_property_photo")
class PropertyPhotoEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "property_id", nullable = false)
    var propertyId: UUID,

    @Column(name = "url", nullable = false, length = 1000)
    var url: String,

    @Column(name = "position", nullable = false)
    var position: Int,

    @Column(name = "created_at", nullable = false)
    var createdAt: java.time.Instant,
)

@Entity
@Table(name = "accommodation_unit_photo")
class UnitPhotoEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "unit_id", nullable = false)
    var unitId: UUID,

    @Column(name = "url", nullable = false, length = 1000)
    var url: String,

    @Column(name = "position", nullable = false)
    var position: Int,

    @Column(name = "created_at", nullable = false)
    var createdAt: java.time.Instant,
)
