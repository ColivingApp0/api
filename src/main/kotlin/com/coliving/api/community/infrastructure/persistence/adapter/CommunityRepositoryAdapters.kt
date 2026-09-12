package com.coliving.api.community.infrastructure.persistence.adapter

import com.coliving.api.community.domain.enums.ParticipationStatus
import com.coliving.api.community.domain.model.ActivityParticipant
import com.coliving.api.community.domain.model.CommunityActivity
import com.coliving.api.community.domain.repository.ActivityParticipantRepository
import com.coliving.api.community.domain.repository.ActivityRepository
import com.coliving.api.community.infrastructure.persistence.mapper.CommunityMappers
import com.coliving.api.community.infrastructure.persistence.repository.ActivityJpaRepository
import com.coliving.api.community.infrastructure.persistence.repository.ActivityParticipantJpaRepository
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class ActivityRepositoryAdapter(
    private val jpaRepository: ActivityJpaRepository,
) : ActivityRepository {

    override fun findById(id: UUID): CommunityActivity? =
        jpaRepository.findById(id).map(CommunityMappers::toDomain).orElse(null)

    override fun findByIdForUpdate(id: UUID): CommunityActivity? =
        jpaRepository.findByIdForUpdate(id)?.let(CommunityMappers::toDomain)

    override fun findByProperty(propertyId: UUID): List<CommunityActivity> =
        CommunityMappers.toDomainList(jpaRepository.findByPropertyIdOrderByScheduledAtDesc(propertyId))

    override fun findByProperties(propertyIds: List<UUID>): List<CommunityActivity> =
        if (propertyIds.isEmpty()) {
            emptyList()
        } else {
            CommunityMappers.toDomainList(jpaRepository.findByPropertyIdInOrderByScheduledAtDesc(propertyIds))
        }

    override fun findByIds(ids: List<UUID>): List<CommunityActivity> =
        if (ids.isEmpty()) {
            emptyList()
        } else {
            CommunityMappers.toDomainList(jpaRepository.findByIdInOrderByScheduledAtDesc(ids))
        }

    @Transactional(propagation = Propagation.MANDATORY)
    override fun save(activity: CommunityActivity) {
        jpaRepository.save(CommunityMappers.toEntity(activity))
    }
}

@Component
class ActivityParticipantRepositoryAdapter(
    private val jpaRepository: ActivityParticipantJpaRepository,
) : ActivityParticipantRepository {

    override fun findByActivity(activityId: UUID): List<ActivityParticipant> =
        CommunityMappers.toParticipantList(
            jpaRepository.findByActivityIdOrderByRegisteredAtAsc(activityId),
        )

    override fun findByActivityAndUser(activityId: UUID, userId: UUID): ActivityParticipant? =
        jpaRepository.findByActivityIdAndUserId(activityId, userId)?.let(CommunityMappers::toDomain)

    override fun findByUser(userId: UUID): List<ActivityParticipant> =
        CommunityMappers.toParticipantList(
            jpaRepository.findByUserIdOrderByRegisteredAtDesc(userId),
        )

    override fun findConfirmedByActivity(activityId: UUID): List<ActivityParticipant> =
        CommunityMappers.toParticipantList(
            jpaRepository.findByActivityIdAndStatus(activityId, ParticipationStatus.CONFIRMADA),
        )

    @Transactional(propagation = Propagation.MANDATORY)
    override fun save(participant: ActivityParticipant) {
        jpaRepository.save(CommunityMappers.toEntity(participant))
    }
}