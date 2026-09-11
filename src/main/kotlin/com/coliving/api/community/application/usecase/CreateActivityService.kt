package com.coliving.api.community.application.usecase

import com.coliving.api.community.application.dto.ActivityDetailView
import com.coliving.api.community.application.dto.ActivityView
import com.coliving.api.community.application.dto.CreateActivityCommand
import com.coliving.api.community.application.dto.ParticipantView
import com.coliving.api.community.application.port.out.CommunityResidencyPort
import com.coliving.api.community.application.port.out.PropertyCommunityPort
import com.coliving.api.community.domain.model.ActivityParticipant
import com.coliving.api.community.domain.model.CommunityActivity
import com.coliving.api.community.domain.repository.ActivityParticipantRepository
import com.coliving.api.community.domain.repository.ActivityRepository
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.InvalidArgumentException
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates the activities of a property (RF-060). The host describes the
 * activity and the enabled participants, which must belong to the property's
 * authorized community: the residents with a confirmed stay (RF-061) — nobody
 * outside it can see the activity.
 */
@Service
class CreateActivityService(
    private val activityRepository: ActivityRepository,
    private val participantRepository: ActivityParticipantRepository,
    private val propertyCommunityPort: PropertyCommunityPort,
    private val residencyPort: CommunityResidencyPort,
) {

    @Transactional
    fun create(command: CreateActivityCommand): ActivityDetailView {
        val hostId = propertyCommunityPort.hostOfProperty(command.propertyId)
            ?: throw NotFoundException("Property not found")
        if (hostId != command.hostId) {
            throw ForbiddenException("Only the host of the property creates its activities")
        }

        val enabled = command.enabledParticipantIds.distinct()
        val residents = residencyPort.residentIdsOfProperty(command.propertyId).toSet()
        val outsiders = enabled.filterNot { it in residents }
        if (outsiders.isNotEmpty()) {
            throw InvalidArgumentException(
                "Enabled participants must belong to the property's community",
            )
        }

        val now = Instant.now()
        val activity = CommunityActivity.create(
            id = UUID.randomUUID(),
            propertyId = command.propertyId,
            hostId = hostId,
            title = command.title,
            description = command.description,
            scheduledAt = command.scheduledAt,
            capacity = command.capacity,
            enabledParticipantIds = enabled,
            now = now,
        )
        activityRepository.save(activity)

        val participants = enabled.map { userId ->
            ActivityParticipant.enable(
                id = UUID.randomUUID(),
                activityId = activity.id,
                userId = userId,
                now = now,
            ).also { participantRepository.save(it) }
        }

        return ActivityDetailView(
            activity = activity.toView(confirmedCount = 0),
            participants = participants.map {
                ParticipantView(userId = it.userId, status = it.status, registeredAt = it.registeredAt)
            },
        )
    }
}

/** Shared activity projection: free places are always derived from the capacity. */
internal fun CommunityActivity.toView(confirmedCount: Int): ActivityView =
    ActivityView(
        id = id,
        propertyId = propertyId,
        hostId = hostId,
        title = title,
        description = description,
        scheduledAt = scheduledAt,
        capacity = capacity,
        status = status,
        confirmedCount = confirmedCount,
        freePlaces = (capacity - confirmedCount).coerceAtLeast(0),
        createdAt = createdAt,
    )