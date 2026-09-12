package com.coliving.api.community.domain.model

import com.coliving.api.community.domain.enums.CommunityActivityStatus
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.InvalidArgumentException
import java.time.Instant
import java.util.UUID

/**
 * Community activity of a property (Actividad, SRS RF-060): the host describes
 * an activity with a date, a capacity and the enabled participants — the
 * authorized community of the property. It stays visible only to those enabled
 * participants until the host cancels it.
 */
class CommunityActivity(
    val id: UUID,
    val propertyId: UUID,
    val hostId: UUID,
    var title: String,
    var description: String?,
    var scheduledAt: Instant,
    var capacity: Int,
    var status: CommunityActivityStatus,
    val createdAt: Instant,
) {

    /** Only the host of the property manages the activity (RF-060). */
    fun requireHost(userId: UUID) {
        if (userId != hostId) {
            throw ForbiddenException("Only the host of the property manages this activity")
        }
    }

    /** Whether the activity still accepts confirmations (RF-061). */
    fun isOpen(now: Instant): Boolean =
        status == CommunityActivityStatus.PROGRAMADA && scheduledAt.isAfter(now)

    /**
     * Fails unless the activity is open: a cancelled activity or one whose date
     * already passed cannot receive new confirmations.
     */
    fun requireOpen(now: Instant) {
        if (status == CommunityActivityStatus.CANCELADA) {
            throw ConflictException("The activity was cancelled")
        }
        if (!scheduledAt.isAfter(now)) {
            throw ConflictException("The activity date has already passed")
        }
    }

    /** Cancels the activity; an already cancelled one is left untouched. */
    fun cancel(now: Instant) {
        if (status == CommunityActivityStatus.CANCELADA) {
            throw ConflictException("The activity is already cancelled")
        }
        status = CommunityActivityStatus.CANCELADA
    }

    /**
     * Capacity rule of RF-061: the list of confirmed participants never exceeds
     * [capacity]. Checked inside the locked transaction that confirms attendance.
     */
    fun requireRoomFor(confirmedCount: Int) {
        if (confirmedCount >= capacity) {
            throw ConflictException("The activity has no free places left")
        }
    }

    companion object {
        const val MAX_TITLE_LENGTH = 120
        const val MAX_DESCRIPTION_LENGTH = 1000

        fun create(
            id: UUID,
            propertyId: UUID,
            hostId: UUID,
            title: String,
            description: String?,
            scheduledAt: Instant,
            capacity: Int,
            enabledParticipantIds: Collection<UUID>,
            now: Instant,
        ): CommunityActivity {
            val normalizedTitle = title.trim()
            if (normalizedTitle.isEmpty()) {
                throw InvalidArgumentException("Activity title must not be empty")
            }
            if (normalizedTitle.length > MAX_TITLE_LENGTH) {
                throw InvalidArgumentException("Activity title must not exceed $MAX_TITLE_LENGTH characters")
            }
            val normalizedDescription = description?.trim()?.takeIf { it.isNotEmpty() }
            if (normalizedDescription != null && normalizedDescription.length > MAX_DESCRIPTION_LENGTH) {
                throw InvalidArgumentException("Activity description must not exceed $MAX_DESCRIPTION_LENGTH characters")
            }
            // RF-060: the date must be valid — an activity cannot be scheduled in the past.
            if (!scheduledAt.isAfter(now)) {
                throw InvalidArgumentException("Activity date must be in the future")
            }
            if (capacity <= 0) {
                throw InvalidArgumentException("Activity capacity must be positive")
            }
            // RF-060: the activity is only visible to the authorized community,
            // so at least one enabled participant is required to create it.
            if (enabledParticipantIds.isEmpty()) {
                throw InvalidArgumentException("At least one enabled participant is required")
            }
            return CommunityActivity(
                id = id,
                propertyId = propertyId,
                hostId = hostId,
                title = normalizedTitle,
                description = normalizedDescription,
                scheduledAt = scheduledAt,
                capacity = capacity,
                status = CommunityActivityStatus.PROGRAMADA,
                createdAt = now,
            )
        }
    }
}