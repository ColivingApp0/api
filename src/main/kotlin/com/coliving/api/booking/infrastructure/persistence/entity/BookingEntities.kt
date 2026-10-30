package com.coliving.api.booking.infrastructure.persistence.entity

import com.coliving.api.booking.domain.enums.ReservationStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "booking_reservation")
class ReservationEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    // Cross-context references are plain UUIDs (no FK by architecture rule).
    @Column(name = "unit_id", nullable = false)
    var unitId: UUID,

    @Column(name = "publication_id", nullable = false)
    var publicationId: UUID,

    @Column(name = "guest_user_id", nullable = false)
    var guestUserId: UUID,

    @Column(name = "places", nullable = false)
    var places: Int = 1,

    @Column(name = "message", columnDefinition = "TEXT")
    var message: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: ReservationStatus,

    // Economic snapshot (RN-07): frozen at request time.
    @Column(name = "monthly_price", precision = 12, scale = 2)
    var monthlyPrice: BigDecimal?,

    @Column(name = "currency", nullable = false, length = 5)
    var currency: String,

    @Column(name = "price_unit", length = 20)
    var priceUnit: String?,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}

@Entity
@Table(name = "booking_reservation_event")
class ReservationEventEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "reservation_id", nullable = false)
    var reservationId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    var eventType: com.coliving.api.booking.domain.enums.ReservationEventType,

    @Column(name = "actor_user_id", nullable = false)
    var actorUserId: UUID,

    @Column(name = "reason", columnDefinition = "TEXT")
    var reason: String?,

    @Column(name = "occurred_at", nullable = false)
    var occurredAt: Instant,
)
