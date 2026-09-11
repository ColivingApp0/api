package com.coliving.api.community.infrastructure.persistence.entity

import com.coliving.api.community.domain.enums.CommunityActivityStatus
import com.coliving.api.community.domain.enums.ParticipationStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "community_activity")
class ActivityEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    // Cross-context reference to the property: plain UUID (no FK by architecture rule).
    @Column(name = "property_id", nullable = false)
    var propertyId: UUID,

    @Column(name = "host_id", nullable = false)
    var hostId: UUID,

    @Column(name = "title", nullable = false, length = 120)
    var title: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String?,

    @Column(name = "scheduled_at", nullable = false)
    var scheduledAt: Instant,

    @Column(name = "capacity", nullable = false)
    var capacity: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: CommunityActivityStatus,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
)

@Entity
@Table(name = "community_activity_participant")
class ActivityParticipantEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "activity_id", nullable = false)
    var activityId: UUID,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: ParticipationStatus,

    @Column(name = "registered_at", nullable = false)
    var registeredAt: Instant,
)