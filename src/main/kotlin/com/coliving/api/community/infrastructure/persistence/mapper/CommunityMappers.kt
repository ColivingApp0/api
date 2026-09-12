package com.coliving.api.community.infrastructure.persistence.mapper

import com.coliving.api.community.domain.model.ActivityParticipant
import com.coliving.api.community.domain.model.CommunityActivity
import com.coliving.api.community.infrastructure.persistence.entity.ActivityEntity
import com.coliving.api.community.infrastructure.persistence.entity.ActivityParticipantEntity

/**
 * Pure domain <-> entity mapping, mirroring the other contexts: aggregates are
 * rebuilt through their constructor with the persisted state, so storage never
 * bypasses a domain rule that must hold on write.
 */
object CommunityMappers {

    fun toDomain(entity: ActivityEntity): CommunityActivity =
        CommunityActivity(
            id = entity.id,
            propertyId = entity.propertyId,
            hostId = entity.hostId,
            title = entity.title,
            description = entity.description,
            scheduledAt = entity.scheduledAt,
            capacity = entity.capacity,
            status = entity.status,
            createdAt = entity.createdAt,
        )

    fun toEntity(activity: CommunityActivity): ActivityEntity =
        ActivityEntity(
            id = activity.id,
            propertyId = activity.propertyId,
            hostId = activity.hostId,
            title = activity.title,
            description = activity.description,
            scheduledAt = activity.scheduledAt,
            capacity = activity.capacity,
            status = activity.status,
            createdAt = activity.createdAt,
        )

    fun toDomainList(entities: List<ActivityEntity>): List<CommunityActivity> =
        entities.map { toDomain(it) }

    fun toDomain(entity: ActivityParticipantEntity): ActivityParticipant =
        ActivityParticipant(
            id = entity.id,
            activityId = entity.activityId,
            userId = entity.userId,
            status = entity.status,
            registeredAt = entity.registeredAt,
        )

    fun toEntity(participant: ActivityParticipant): ActivityParticipantEntity =
        ActivityParticipantEntity(
            id = participant.id,
            activityId = participant.activityId,
            userId = participant.userId,
            status = participant.status,
            registeredAt = participant.registeredAt,
        )

    fun toParticipantList(entities: List<ActivityParticipantEntity>): List<ActivityParticipant> =
        entities.map { toDomain(it) }
}