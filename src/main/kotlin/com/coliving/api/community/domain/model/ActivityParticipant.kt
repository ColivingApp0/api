package com.coliving.api.community.domain.model

import com.coliving.api.community.domain.enums.ParticipationStatus
import com.coliving.api.shared.error.ConflictException
import java.time.Instant
import java.util.UUID

/**
 * Participation of a resident in a [CommunityActivity] (ParticipanteActividad,
 * SRS RF-060/RF-061). A row exists from the moment the host enables the
 * resident, so the authorized community of an activity is explicit: only a user
 * with a participant row is part of it (the application service relies on that
 * row to authorize reads and writes).
 */
class ActivityParticipant(
    val id: UUID,
    val activityId: UUID,
    val userId: UUID,
    var status: ParticipationStatus,
    var registeredAt: Instant,
) {

    fun isConfirmed(): Boolean = status == ParticipationStatus.CONFIRMADA

    /** Confirms attendance (RF-061); an already confirmed participant is a conflict. */
    fun confirm(now: Instant) {
        if (status == ParticipationStatus.CONFIRMADA) {
            throw ConflictException("Attendance is already confirmed")
        }
        status = ParticipationStatus.CONFIRMADA
        registeredAt = now
    }

    /** Withdraws from the activity (RF-061), freeing a place of the capacity. */
    fun withdraw(now: Instant) {
        if (status != ParticipationStatus.CONFIRMADA) {
            throw ConflictException("Attendance is not confirmed")
        }
        status = ParticipationStatus.RETIRADA
        registeredAt = now
    }

    companion object {
        /** Enabled at creation time: the activity is visible to this resident (RF-060). */
        fun enable(
            id: UUID,
            activityId: UUID,
            userId: UUID,
            now: Instant,
        ): ActivityParticipant = ActivityParticipant(
            id = id,
            activityId = activityId,
            userId = userId,
            status = ParticipationStatus.HABILITADO,
            registeredAt = now,
        )
    }
}