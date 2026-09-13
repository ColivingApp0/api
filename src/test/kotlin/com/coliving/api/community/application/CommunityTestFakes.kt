package com.coliving.api.community.application

import com.coliving.api.community.application.port.out.CommunityConsentPort
import com.coliving.api.community.application.port.out.CommunityResidencyPort
import com.coliving.api.community.application.port.out.PropertyCommunityPort
import com.coliving.api.community.domain.model.ActivityParticipant
import com.coliving.api.community.domain.model.CommunityActivity
import com.coliving.api.community.domain.repository.ActivityParticipantRepository
import com.coliving.api.community.domain.repository.ActivityRepository
import java.util.UUID

/** In-memory stand-in of accommodation's PropertyCommunityQuery (RF-060). */
class FakePropertyCommunityPort : PropertyCommunityPort {
    val owners = mutableMapOf<UUID, UUID>()
    private val units = mutableMapOf<UUID, List<UUID>>()

    override fun hostOfProperty(propertyId: UUID): UUID? = owners[propertyId]

    override fun propertyIdsOfHost(hostId: UUID): List<UUID> =
        owners.filterValues { it == hostId }.keys.toList()

    override fun unitIdsOfProperty(propertyId: UUID): List<UUID> = units[propertyId].orEmpty()

    fun addProperty(propertyId: UUID, hostId: UUID, unitIds: List<UUID> = emptyList()) {
        owners[propertyId] = hostId
        units[propertyId] = unitIds
    }
}

/** In-memory stand-in of booking's ReservationCommunityQuery (RF-061). */
class FakeCommunityResidencyPort : CommunityResidencyPort {
    val residents = mutableMapOf<UUID, List<UUID>>()

    override fun residentIdsOfProperty(propertyId: UUID): List<UUID> =
        residents[propertyId].orEmpty()

    fun setResidents(propertyId: UUID, vararg userIds: UUID) {
        residents[propertyId] = userIds.toList()
    }
}

/** In-memory stand-in of identity's consent projection (RF-062). */
class FakeCommunityConsentPort : CommunityConsentPort {
    val consented = mutableSetOf<UUID>()

    override fun hasDataSharingConsent(userId: UUID): Boolean = userId in consented
}

class FakeActivityRepository : ActivityRepository {
    val store = mutableMapOf<UUID, CommunityActivity>()

    override fun findById(id: UUID): CommunityActivity? = store[id]

    override fun findByIdForUpdate(id: UUID): CommunityActivity? = store[id]

    override fun findByProperty(propertyId: UUID): List<CommunityActivity> =
        store.values.filter { it.propertyId == propertyId }.sortedByDescending { it.scheduledAt }

    override fun findByProperties(propertyIds: List<UUID>): List<CommunityActivity> =
        store.values.filter { it.propertyId in propertyIds }.sortedByDescending { it.scheduledAt }

    override fun findByIds(ids: List<UUID>): List<CommunityActivity> =
        store.values.filter { it.id in ids }.sortedByDescending { it.scheduledAt }

    override fun save(activity: CommunityActivity) {
        store[activity.id] = activity
    }
}

class FakeActivityParticipantRepository : ActivityParticipantRepository {
    val store = mutableListOf<ActivityParticipant>()

    override fun findByActivity(activityId: UUID): List<ActivityParticipant> =
        store.filter { it.activityId == activityId }.sortedBy { it.registeredAt }

    override fun findByActivityAndUser(activityId: UUID, userId: UUID): ActivityParticipant? =
        store.firstOrNull { it.activityId == activityId && it.userId == userId }

    override fun findByUser(userId: UUID): List<ActivityParticipant> =
        store.filter { it.userId == userId }.sortedByDescending { it.registeredAt }

    override fun findConfirmedByActivity(activityId: UUID): List<ActivityParticipant> =
        findByActivity(activityId).filter { it.isConfirmed() }

    override fun save(participant: ActivityParticipant) {
        store.removeIf { it.id == participant.id }
        store.add(participant)
    }
}