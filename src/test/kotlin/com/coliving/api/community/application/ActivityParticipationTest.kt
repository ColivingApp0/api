package com.coliving.api.community.application

import com.coliving.api.community.application.dto.CancelActivityCommand
import com.coliving.api.community.application.dto.ConfirmAttendanceCommand
import com.coliving.api.community.application.dto.CreateActivityCommand
import com.coliving.api.community.application.dto.WithdrawFromActivityCommand
import com.coliving.api.community.application.usecase.CancelActivityService
import com.coliving.api.community.application.usecase.ConfirmAttendanceService
import com.coliving.api.community.application.usecase.CreateActivityService
import com.coliving.api.community.application.usecase.ListActivitiesService
import com.coliving.api.community.application.usecase.WithdrawFromActivityService
import com.coliving.api.community.domain.enums.CommunityActivityStatus
import com.coliving.api.community.domain.enums.ParticipationStatus
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Attendance flow: confirm, capacity, withdraw (RF-061). */
class ActivityParticipationTest {

    private val activities = FakeActivityRepository()
    private val participants = FakeActivityParticipantRepository()
    private val properties = FakePropertyCommunityPort()
    private val residency = FakeCommunityResidencyPort()
    private val createService = CreateActivityService(activities, participants, properties, residency)
    private val confirmService = ConfirmAttendanceService(activities, participants)
    private val withdrawService = WithdrawFromActivityService(activities, participants)
    private val cancelService = CancelActivityService(activities, participants)
    private val listService = ListActivitiesService(activities, participants, properties)

    private val hostId = UUID.randomUUID()
    private val residentA = UUID.randomUUID()
    private val residentB = UUID.randomUUID()
    private val propertyId = UUID.randomUUID()

    /** Creates an activity with the given capacity and returns its id. */
    private fun givenActivity(capacity: Int = 1): UUID {
        properties.addProperty(propertyId = propertyId, hostId = hostId, unitIds = listOf(UUID.randomUUID()))
        residency.setResidents(propertyId, residentA, residentB)
        return createService.create(
            CreateActivityCommand(
                hostId = hostId,
                propertyId = propertyId,
                title = "Clase de cocina",
                description = null,
                scheduledAt = Instant.now().plusSeconds(86_400 * 2),
                capacity = capacity,
                enabledParticipantIds = listOf(residentA, residentB),
            ),
        ).activity.id
    }

    private fun confirm(activityId: UUID, userId: UUID) =
        confirmService.confirm(ConfirmAttendanceCommand(activityId, userId))

    @Test
    fun `a resident confirms attendance and the counters follow (RF-061)`() {
        val activityId = givenActivity(capacity = 2)

        val confirmation = confirm(activityId, residentA)

        assertEquals(ParticipationStatus.CONFIRMADA, confirmation.status)
        val view = listService.listMine(residentA).single()
        assertEquals(1, view.confirmedCount)
        assertEquals(1, view.freePlaces)
    }

    @Test
    fun `the capacity cannot be exceeded (RF-061)`() {
        val activityId = givenActivity(capacity = 1)
        confirm(activityId, residentA)

        assertFailsWith<ConflictException> { confirm(activityId, residentB) }
        assertEquals(1, participants.findConfirmedByActivity(activityId).size)
    }

    @Test
    fun `withdrawing frees the place for another resident (RF-061)`() {
        val activityId = givenActivity(capacity = 1)
        confirm(activityId, residentA)

        val withdrawn = withdrawService.withdraw(WithdrawFromActivityCommand(activityId, residentA))
        assertEquals(ParticipationStatus.RETIRADA, withdrawn.status)

        assertEquals(ParticipationStatus.CONFIRMADA, confirm(activityId, residentB).status)
    }

    @Test
    fun `repeated confirmations and withdrawals without a confirmation are conflicts`() {
        val activityId = givenActivity()

        assertFailsWith<ConflictException> {
            withdrawService.withdraw(WithdrawFromActivityCommand(activityId, residentA))
        }
        confirm(activityId, residentA)
        assertFailsWith<ConflictException> { confirm(activityId, residentA) }
    }

    @Test
    fun `a user outside the authorized community cannot read nor confirm (RF-060)`() {
        val activityId = givenActivity()
        val outsider = UUID.randomUUID()

        assertFailsWith<ForbiddenException> { confirm(activityId, outsider) }
        assertFailsWith<ForbiddenException> { listService.detail(activityId, outsider) }
        assertTrue { listService.listMine(outsider).isEmpty() }
    }

    @Test
    fun `a cancelled activity accepts no confirmations`() {
        val activityId = givenActivity()
        cancelService.cancel(CancelActivityCommand(activityId, hostId))

        assertFailsWith<ConflictException> { confirm(activityId, residentA) }
        assertEquals(CommunityActivityStatus.CANCELADA, activities.findById(activityId)!!.status)
    }

    @Test
    fun `the host sees the activities of their properties and only the host cancels them`() {
        val activityId = givenActivity()
        confirm(activityId, residentA)

        val hostView = listService.listForHost(hostId).single()
        assertEquals(1, hostView.confirmedCount)
        assertTrue { listService.listForHost(UUID.randomUUID()).isEmpty() }
        assertFailsWith<ForbiddenException> {
            cancelService.cancel(CancelActivityCommand(activityId, residentA))
        }
    }

    @Test
    fun `the detail read exposes the participants of the activity to its community`() {
        val activityId = givenActivity()
        confirm(activityId, residentA)

        val detail = listService.detail(activityId, residentB)

        assertEquals(2, detail.participants.size)
        assertEquals(
            listOf(residentA),
            detail.participants.filter { it.status == ParticipationStatus.CONFIRMADA }.map { it.userId },
        )
    }
}