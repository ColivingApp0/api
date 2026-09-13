package com.coliving.api.community.application.usecase

import com.coliving.api.community.application.dto.CommunitySummaryView
import com.coliving.api.community.application.port.out.CommunityConsentPort
import com.coliving.api.community.application.port.out.CommunityResidencyPort
import com.coliving.api.community.domain.enums.CommunityActivityStatus
import com.coliving.api.community.domain.repository.ActivityRepository
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Aggregated information about the resident community of a property (RF-062,
 * RN-05). It exposes counts only: the number of residents, how many of them
 * consented to share their information, and how many activities are still
 * ahead. Names, pictures and contact data are never returned, and the summary
 * is only available when at least one resident consented
 * (`available = false` otherwise).
 */
@Service
class CommunitySummaryService(
    private val activityRepository: ActivityRepository,
    private val residencyPort: CommunityResidencyPort,
    private val consentPort: CommunityConsentPort,
) {

    @Transactional(readOnly = true)
    fun summaryOf(propertyId: UUID): CommunitySummaryView {
        val residents = residencyPort.residentIdsOfProperty(propertyId)
        val consentedResidentCount = residents.count { consentPort.hasDataSharingConsent(it) }
        return CommunitySummaryView(
            propertyId = propertyId,
            available = consentedResidentCount > 0,
            residentCount = residents.size,
            consentedResidentCount = consentedResidentCount,
            upcomingActivityCount = upcomingActivities(propertyId),
        )
    }

    private fun upcomingActivities(propertyId: UUID): Int {
        val now = Instant.now()
        return activityRepository.findByProperty(propertyId).count {
            it.status == CommunityActivityStatus.PROGRAMADA && it.scheduledAt.isAfter(now)
        }
    }
}