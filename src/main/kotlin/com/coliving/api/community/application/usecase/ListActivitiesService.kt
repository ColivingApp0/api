package com.coliving.api.community.application.usecase

import com.coliving.api.community.application.dto.ActivityDetailView
import com.coliving.api.community.application.dto.ActivityView
import com.coliving.api.community.application.dto.ParticipantView
import com.coliving.api.community.application.port.out.PropertyCommunityPort
import com.coliving.api.community.domain.model.CommunityActivity
import com.coliving.api.community.domain.repository.ActivityParticipantRepository
import com.coliving.api.community.domain.repository.ActivityRepository
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.NotFoundException
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read side of the community activities. A host sees the activities of their
 * properties; a resident sees the activities they were enabled in — the
 * authorized community of an activity (RF-060) — and nobody else can read it.
 */
@Service
class ListActivitiesService(
    private val activityRepository: ActivityRepository,
    private val participantRepository: ActivityParticipantRepository,
    private val propertyCommunityPort: PropertyCommunityPort,
) {

    @Transactional(readOnly = true)
    fun listForHost(hostId: UUID): List<ActivityView> {
        val propertyIds = propertyCommunityPort.propertyIdsOfHost(hostId)
        if (propertyIds.isEmpty()) return emptyList()
        return activityRepository.findByProperties(propertyIds).map { it.toView(confirmed(it.id)) }
    }

    @Transactional(readOnly = true)
    fun listMine(userId: UUID): List<ActivityView> {
        val activityIds = participantRepository.findByUser(userId).map { it.activityId }
        if (activityIds.isEmpty()) return emptyList()
        return activityRepository.findByIds(activityIds).map { it.toView(confirmed(it.id)) }
    }

    @Transactional(readOnly = true)
    fun detail(activityId: UUID, requesterId: UUID): ActivityDetailView {
        val activity = activityRepository.findById(activityId)
            ?: throw NotFoundException("Activity not found")
        val participants = participantRepository.findByActivity(activityId)
        val allowed = activity.hostId == requesterId || participants.any { it.userId == requesterId }
        if (!allowed) {
            throw ForbiddenException("Not part of the authorized community of this activity")
        }
        return ActivityDetailView(
            activity = activity.toView(participants.count { it.isConfirmed() }),
            participants = participants.map {
                ParticipantView(userId = it.userId, status = it.status, registeredAt = it.registeredAt)
            },
        )
    }

    private fun confirmed(activityId: UUID): Int =
        participantRepository.findConfirmedByActivity(activityId).size
}