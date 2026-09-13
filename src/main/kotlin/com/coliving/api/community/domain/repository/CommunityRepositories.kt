package com.coliving.api.community.domain.repository

import com.coliving.api.community.domain.model.ActivityParticipant
import com.coliving.api.community.domain.model.CommunityActivity
import java.util.UUID

/**
 * Activity persistence port. Implementations must promise the locking
 * discipline of [findByIdForUpdate] so concurrent confirmations cannot exceed
 * the capacity (RF-061).
 */
interface ActivityRepository {

    fun findById(id: UUID): CommunityActivity?

    /**
     * Reads the activity holding a write lock on its row (`SELECT ... FOR
     * UPDATE`) for the duration of the transaction. Confirming attendance
     * happens inside that transaction, which serializes the capacity check.
     */
    fun findByIdForUpdate(id: UUID): CommunityActivity?

    /** Activities of a property, newest first. */
    fun findByProperty(propertyId: UUID): List<CommunityActivity>

    /** Activities of any of the given properties, newest first. */
    fun findByProperties(propertyIds: List<UUID>): List<CommunityActivity>

    /**
     * Activities with the given ids, ordered newest first. Used to list the
     * activities a resident is enabled in without one query per activity.
     */
    fun findByIds(ids: List<UUID>): List<CommunityActivity>

    fun save(activity: CommunityActivity)
}

interface ActivityParticipantRepository {

    /** Participants of an activity ordered by registration. */
    fun findByActivity(activityId: UUID): List<ActivityParticipant>

    fun findByActivityAndUser(activityId: UUID, userId: UUID): ActivityParticipant?

    /** Activities the user is enabled in (their authorized community), newest first. */
    fun findByUser(userId: UUID): List<ActivityParticipant>

    /** Participants of an activity that confirmed attendance (RF-061). */
    fun findConfirmedByActivity(activityId: UUID): List<ActivityParticipant>

    fun save(participant: ActivityParticipant)
}