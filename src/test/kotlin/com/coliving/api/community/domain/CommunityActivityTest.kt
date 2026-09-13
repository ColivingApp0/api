package com.coliving.api.community.domain

import com.coliving.api.community.domain.enums.CommunityActivityStatus
import com.coliving.api.community.domain.enums.ParticipationStatus
import com.coliving.api.community.domain.model.ActivityParticipant
import com.coliving.api.community.domain.model.CommunityActivity
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.InvalidArgumentException
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Invariants of the community aggregates (RF-060, RF-061). */
class CommunityActivityTest {

    private val hostId = UUID.randomUUID()
    private val residentId = UUID.randomUUID()
    private val now = Instant.parse("2026-09-10T14:00:00Z")

    private fun activity(
        scheduledAt: Instant = now.plusSeconds(86_400),
        capacity: Int = 2,
        enabled: Collection<UUID> = listOf(residentId),
    ): CommunityActivity = CommunityActivity.create(
        id = UUID.randomUUID(),
        propertyId = UUID.randomUUID(),
        hostId = hostId,
        title = "  Asado de bienvenida  ",
        description = "  Trae algo para compartir  ",
        scheduledAt = scheduledAt,
        capacity = capacity,
        enabledParticipantIds = enabled,
        now = now,
    )

    @Test
    fun `creates a published activity for the enabled community (RF-060)`() {
        val activity = activity()

        assertEquals("Asado de bienvenida", activity.title)
        assertEquals("Trae algo para compartir", activity.description)
        assertEquals(CommunityActivityStatus.PROGRAMADA, activity.status)
        assertTrue { activity.isOpen(now) }
    }

    @Test
    fun `rejects invalid dates, capacity and empty community (RF-060)`() {
        // No admite fechas inválidas: ni pasadas ni presentes.
        assertFailsWith<InvalidArgumentException> { activity(scheduledAt = now) }
        assertFailsWith<InvalidArgumentException> { activity(scheduledAt = now.minusSeconds(60)) }
        assertFailsWith<InvalidArgumentException> { activity(capacity = 0) }
        assertFailsWith<InvalidArgumentException> { activity(enabled = emptyList()) }
        assertFailsWith<InvalidArgumentException> {
            CommunityActivity.create(
                UUID.randomUUID(), UUID.randomUUID(), hostId, "   ", null,
                now.plusSeconds(60), 1, listOf(residentId), now,
            )
        }
    }

    @Test
    fun `only the host manages the activity`() {
        val activity = activity()

        activity.requireHost(hostId)
        assertFailsWith<ForbiddenException> { activity.requireHost(residentId) }
    }

    @Test
    fun `cancelling is terminal and cannot be repeated`() {
        val activity = activity()

        activity.cancel(now)

        assertEquals(CommunityActivityStatus.CANCELADA, activity.status)
        assertFailsWith<ConflictException> { activity.cancel(now) }
        assertFailsWith<ConflictException> { activity.requireOpen(now) }
    }

    @Test
    fun `capacity limits confirmations (RF-061)`() {
        val activity = activity(capacity = 1)

        activity.requireRoomFor(0)
        assertFailsWith<ConflictException> { activity.requireRoomFor(1) }
        assertFailsWith<ConflictException> { activity.requireRoomFor(5) }
    }

    @Test
    fun `an activity whose date passed accepts no more confirmations`() {
        val activity = activity()
        val after = now.plusSeconds(86_400 * 2)

        assertFailsWith<ConflictException> { activity.requireOpen(after) }
        assertTrue { !activity.isOpen(after) }
    }

    @Test
    fun `participants confirm and withdraw, freeing the place (RF-061)`() {
        val participant = ActivityParticipant.enable(UUID.randomUUID(), UUID.randomUUID(), residentId, now)
        assertEquals(ParticipationStatus.HABILITADO, participant.status)

        val confirmedAt = now.plusSeconds(600)
        participant.confirm(confirmedAt)
        assertEquals(ParticipationStatus.CONFIRMADA, participant.status)
        assertEquals(confirmedAt, participant.registeredAt)
        assertTrue { participant.isConfirmed() }
        assertFailsWith<ConflictException> { participant.confirm(confirmedAt) }

        participant.withdraw(confirmedAt.plusSeconds(60))
        assertEquals(ParticipationStatus.RETIRADA, participant.status)
        assertTrue { !participant.isConfirmed() }
        assertFailsWith<ConflictException> { participant.withdraw(confirmedAt) }
        // Withdrawing frees the place: the resident can confirm again.
        participant.confirm(confirmedAt.plusSeconds(120))
        assertEquals(ParticipationStatus.CONFIRMADA, participant.status)
    }
}