package com.coliving.api.community.application.usecase

import com.coliving.api.community.application.dto.ConfirmAttendanceCommand
import com.coliving.api.community.application.dto.ParticipantView
import com.coliving.api.community.domain.repository.ActivityParticipantRepository
import com.coliving.api.community.domain.repository.ActivityRepository
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Confirms attendance at an activity (RF-061). The capacity rule is enforced
 * inside the transaction that holds a write lock on the activity row, so
 * concurrent confirmations can never exceed the available places.
 */
@Service
class ConfirmAttendanceService(
    private val activityRepository: ActivityRepository,
    private val participantRepository: ActivityParticipantRepository,
) {

    @Transactional
    fun confirm(command: ConfirmAttendanceCommand): ParticipantView {
        // Locking read: serializes the capacity check that follows.
        val activity = activityRepository.findByIdForUpdate(command.activityId)
            ?: throw NotFoundException("Activity not found")
        activity.requireOpen(Instant.now())

        // Only a participant enabled by the host belongs to the authorized community.
        val participant = participantRepository.findByActivityAndUser(activity.id, command.userId)
            ?: throw ForbiddenException("Not part of the authorized community of this activity")

        if (!participant.isConfirmed()) {
            val confirmedCount = participantRepository.findConfirmedByActivity(activity.id).size
            activity.requireRoomFor(confirmedCount)
        }
        participant.confirm(Instant.now())
        participantRepository.save(participant)

        return ParticipantView(
            userId = participant.userId,
            status = participant.status,
            registeredAt = participant.registeredAt,
        )
    }
}