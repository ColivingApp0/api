package com.coliving.api.community.application.usecase

import com.coliving.api.community.application.dto.ParticipantView
import com.coliving.api.community.application.dto.WithdrawFromActivityCommand
import com.coliving.api.community.domain.repository.ActivityParticipantRepository
import com.coliving.api.community.domain.repository.ActivityRepository
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Withdraws from an activity (RF-061). Withdrawing frees a place of the
 * capacity, so the participating list and the free places stay consistent for
 * the next confirmation.
 */
@Service
class WithdrawFromActivityService(
    private val activityRepository: ActivityRepository,
    private val participantRepository: ActivityParticipantRepository,
) {

    @Transactional
    fun withdraw(command: WithdrawFromActivityCommand): ParticipantView {
        val activity = activityRepository.findByIdForUpdate(command.activityId)
            ?: throw NotFoundException("Activity not found")
        val participant = participantRepository.findByActivityAndUser(activity.id, command.userId)
            ?: throw ForbiddenException("Not part of the authorized community of this activity")

        participant.withdraw(Instant.now())
        participantRepository.save(participant)

        return ParticipantView(
            userId = participant.userId,
            status = participant.status,
            registeredAt = participant.registeredAt,
        )
    }
}