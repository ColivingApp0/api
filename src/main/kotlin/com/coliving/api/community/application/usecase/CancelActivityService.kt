package com.coliving.api.community.application.usecase

import com.coliving.api.community.application.dto.ActivityView
import com.coliving.api.community.application.dto.CancelActivityCommand
import com.coliving.api.community.domain.repository.ActivityParticipantRepository
import com.coliving.api.community.domain.repository.ActivityRepository
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Cancels an activity (RF-060). Only the host of the property can do it; the
 * activity and its participant list are preserved so the community keeps a
 * truthful record of what was scheduled.
 */
@Service
class CancelActivityService(
    private val activityRepository: ActivityRepository,
    private val participantRepository: ActivityParticipantRepository,
) {

    @Transactional
    fun cancel(command: CancelActivityCommand): ActivityView {
        val activity = activityRepository.findById(command.activityId)
            ?: throw NotFoundException("Activity not found")
        activity.requireHost(command.actorId)
        activity.cancel(Instant.now())
        activityRepository.save(activity)

        return activity.toView(participantRepository.findConfirmedByActivity(activity.id).size)
    }
}