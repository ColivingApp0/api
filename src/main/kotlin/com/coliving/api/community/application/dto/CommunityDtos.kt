package com.coliving.api.community.application.dto

import com.coliving.api.community.domain.enums.CommunityActivityStatus
import com.coliving.api.community.domain.enums.ParticipationStatus
import java.time.Instant
import java.util.UUID

/** Activity as returned to the host and to its authorized community (RF-060). */
data class ActivityView(
    val id: UUID,
    val propertyId: UUID,
    val hostId: UUID,
    val title: String,
    val description: String?,
    val scheduledAt: Instant,
    val capacity: Int,
    val status: CommunityActivityStatus,
    val confirmedCount: Int,
    val freePlaces: Int,
    val createdAt: Instant,
)

/** One participant of an activity with the current state of their attendance. */
data class ParticipantView(
    val userId: UUID,
    val status: ParticipationStatus,
    val registeredAt: Instant,
)

/** Activity plus its participants; the host sees the full authorized community. */
data class ActivityDetailView(
    val activity: ActivityView,
    val participants: List<ParticipantView>,
)

/** Command to create an activity for one of the host's properties (RF-060). */
data class CreateActivityCommand(
    val hostId: UUID,
    val propertyId: UUID,
    val title: String,
    val description: String?,
    val scheduledAt: Instant,
    val capacity: Int,
    val enabledParticipantIds: List<UUID>,
)

/** Command to cancel an activity; only its host can do it. */
data class CancelActivityCommand(
    val activityId: UUID,
    val actorId: UUID,
)

/** Command to confirm attendance at an activity (RF-061). */
data class ConfirmAttendanceCommand(
    val activityId: UUID,
    val userId: UUID,
)

/** Command to withdraw from an activity (RF-061). */
data class WithdrawFromActivityCommand(
    val activityId: UUID,
    val userId: UUID,
)

/**
 * Aggregated view of the resident community of a property (RF-062). It carries
 * counts only — never names, pictures or contact data — and is only exposed
 * when enough residents consented to share their information.
 */
data class CommunitySummaryView(
    val propertyId: UUID,
    val available: Boolean,
    val residentCount: Int,
    val consentedResidentCount: Int,
    val upcomingActivityCount: Int,
)