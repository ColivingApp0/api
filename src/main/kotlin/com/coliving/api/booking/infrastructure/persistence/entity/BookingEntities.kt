package com.coliving.api.booking.infrastructure.persistence.entity

import com.coliving.api.booking.domain.enums.CancellationPolicyType
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
import java.time.LocalDate
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

    @Column(name = "from_date", nullable = false)
    var fromDate: LocalDate,

    @Column(name = "to_date", nullable = false)
    var toDate: LocalDate,

    @Column(name = "occupants", nullable = false)
    var occupants: Int,

    @Column(name = "message", columnDefinition = "TEXT")
    var message: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: ReservationStatus,

    // Economic snapshot (RN-07): frozen at request time.
    @Column(name = "price_per_night", nullable = false, precision = 12, scale = 2)
    var pricePerNight: BigDecimal,

    @Column(name = "currency", nullable = false, length = 5)
    var currency: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_policy", length = 30)
    var cancellationPolicy: CancellationPolicyType?,

    @Column(name = "hold_expires_at")
    var holdExpiresAt: Instant?,

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
