package com.coliving.api.accommodation.infrastructure.persistence.entity

import com.coliving.api.accommodation.domain.enums.AvailabilityState
import com.coliving.api.accommodation.domain.enums.CancellationPolicy
import com.coliving.api.accommodation.domain.enums.Currency
import com.coliving.api.accommodation.domain.enums.PublicationStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
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

    @Column(name = "max_guests", nullable = false)
    var maxGuests: Int,

    @Column(name = "bedrooms", nullable = false)
    var bedrooms: Int,

    @Column(name = "beds", nullable = false)
    var beds: Int,

    @Column(name = "bathrooms", nullable = false)
    var bathrooms: Int,

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
)

@Entity
@Table(name = "accommodation_pricing")
class PricingEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "publication_id", nullable = false)
    var publicationId: UUID,

    @Column(name = "base_price_per_night", nullable = false, precision = 12, scale = 2)
    var basePricePerNight: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 5)
    var currency: Currency,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: java.time.Instant,
)

@Entity
@Table(name = "accommodation_rule")
class RuleEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "publication_id", nullable = false)
    var publicationId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_policy", nullable = false, length = 50)
    var cancellationPolicy: CancellationPolicy,

    @Column(name = "check_in_time", nullable = false)
    var checkInTime: LocalTime,

    @Column(name = "check_out_time", nullable = false)
    var checkOutTime: LocalTime,

    @Column(name = "min_nights", nullable = false)
    var minNights: Int,

    @Column(name = "max_nights", nullable = false)
    var maxNights: Int,

    @Column(name = "house_rules", columnDefinition = "TEXT")
    var houseRules: String?,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: java.time.Instant,
)

@Entity
@Table(name = "accommodation_availability_slot")
class AvailabilitySlotEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long = 0L,

    @Column(name = "unit_id", nullable = false)
    var unitId: UUID,

    @Column(name = "date", nullable = false)
    var date: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 30)
    var state: AvailabilityState,

    @Column(name = "reservation_id")
    var reservationId: UUID?,
)