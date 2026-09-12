package com.coliving.api.community.application

import com.coliving.api.community.application.dto.CommunitySummaryView
import com.coliving.api.community.application.dto.CreateActivityCommand
import com.coliving.api.community.application.usecase.CommunitySummaryService
import com.coliving.api.community.application.usecase.CreateActivityService
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Aggregated community information with its consent gate (RF-062, RN-05). */
class CommunitySummaryServiceTest {

    private val activities = FakeActivityRepository()
    private val participants = FakeActivityParticipantRepository()
    private val properties = FakePropertyCommunityPort()
    private val residency = FakeCommunityResidencyPort()
    private val consent = FakeCommunityConsentPort()
    private val summaryService = CommunitySummaryService(activities, residency, consent)
    private val createService = CreateActivityService(activities, participants, properties, residency)

    private val hostId = UUID.randomUUID()
    private val residentA = UUID.randomUUID()
    private val residentB = UUID.randomUUID()
    private val propertyId = UUID.randomUUID()

    private fun givenCommunity() {
        properties.addProperty(propertyId = propertyId, hostId = hostId, unitIds = listOf(UUID.randomUUID()))
        residency.setResidents(propertyId, residentA, residentB)
    }

    private fun givenUpcomingActivity() {
        createService.create(
            CreateActivityCommand(
                hostId = hostId,
                propertyId = propertyId,
                title = "Tarde de juegos",
                description = null,
                scheduledAt = Instant.now().plusSeconds(86_400),
                capacity = 4,
                enabledParticipantIds = listOf(residentA),
            ),
        )
    }

    @Test
    fun `counts residents and activities without exposing identities (RF-062)`() {
        givenCommunity()
        givenUpcomingActivity()
        consent.consented.add(residentA)

        val summary = summaryService.summaryOf(propertyId)

        assertEquals(2, summary.residentCount)
        assertEquals(1, summary.consentedResidentCount)
        assertEquals(1, summary.upcomingActivityCount)
        assertTrue { summary.available }
    }

    @Test
    fun `without consent of any resident the aggregate is not available (RF-062, RN-05)`() {
        givenCommunity()

        val summary = summaryService.summaryOf(propertyId)

        assertFalse(summary.available)
        assertEquals(0, summary.consentedResidentCount)
    }

    @Test
    fun `a property without residents reports zeros`() {
        val summary: CommunitySummaryView = summaryService.summaryOf(UUID.randomUUID())

        assertEquals(0, summary.residentCount)
        assertEquals(0, summary.upcomingActivityCount)
        assertFalse(summary.available)
    }

    @Test
    fun `only activities still ahead are counted`() {
        givenCommunity()
        givenUpcomingActivity()

        // An activity scheduled in the past cannot even be created (RF-060), so
        // the counter only has to discard the cancelled ones.
        val activityId = activities.store.keys.single()
        assertEquals(1, summaryService.summaryOf(propertyId).upcomingActivityCount)

        activities.store[activityId]!!.cancel(Instant.now())

        assertEquals(0, summaryService.summaryOf(propertyId).upcomingActivityCount)
    }
}