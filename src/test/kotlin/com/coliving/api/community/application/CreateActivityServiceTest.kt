package com.coliving.api.community.application

import com.coliving.api.community.application.dto.CreateActivityCommand
import com.coliving.api.community.application.usecase.CreateActivityService
import com.coliving.api.community.domain.enums.ParticipationStatus
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.InvalidArgumentException
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateActivityServiceTest {

    private val activities = FakeActivityRepository()
    private val participants = FakeActivityParticipantRepository()
    private val properties = FakePropertyCommunityPort()
    private val residency = FakeCommunityResidencyPort()
    private val service = CreateActivityService(activities, participants, properties, residency)

    private val hostId = UUID.randomUUID()
    private val residentA = UUID.randomUUID()
    private val residentB = UUID.randomUUID()
    private val propertyId = UUID.randomUUID()
    private val scheduledAt = Instant.now().plusSeconds(86_400 * 3)

    private fun command(
        hostId: UUID = this.hostId,
        propertyId: UUID = this.propertyId,
        enabled: List<UUID> = listOf(residentA, residentB),
        capacity: Int = 4,
    ) = CreateActivityCommand(
        hostId = hostId,
        propertyId = propertyId,
        title = "Caminata al cerro",
        description = "Punto de encuentro en la entrada",
        scheduledAt = scheduledAt,
        capacity = capacity,
        enabledParticipantIds = enabled,
    )

    private fun givenProperty() {
        properties.addProperty(propertyId = propertyId, hostId = hostId, unitIds = listOf(UUID.randomUUID()))
        residency.setResidents(propertyId, residentA, residentB)
    }

    @Test
    fun `host creates the activity and enables the residents (RF-060)`() {
        givenProperty()

        val detail = service.create(command())

        assertEquals(propertyId, detail.activity.propertyId)
        assertEquals(0, detail.activity.confirmedCount)
        assertEquals(4, detail.activity.freePlaces)
        assertEquals(
            listOf(ParticipationStatus.HABILITADO, ParticipationStatus.HABILITADO),
            detail.participants.map { it.status },
        )
        assertEquals(setOf(residentA, residentB), detail.participants.map { it.userId }.toSet())
    }

    @Test
    fun `rejects an unknown property`() {
        assertFailsWith<NotFoundException> { service.create(command()) }
    }

    @Test
    fun `rejects a host that does not own the property (RF-060)`() {
        givenProperty()

        assertFailsWith<ForbiddenException> { service.create(command(hostId = UUID.randomUUID())) }
    }

    @Test
    fun `only the authorized community can be enabled (RF-060)`() {
        givenProperty()

        assertFailsWith<InvalidArgumentException> {
            service.create(command(enabled = listOf(residentA, UUID.randomUUID())))
        }
    }

    @Test
    fun `repeated enabled ids are stored once`() {
        givenProperty()

        val detail = service.create(command(enabled = listOf(residentA, residentA)))

        assertEquals(1, detail.participants.size)
        assertEquals(residentA, detail.participants.single().userId)
    }
}