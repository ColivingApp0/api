package com.coliving.api.messaging.application

import com.coliving.api.messaging.application.dto.OpenConversationCommand
import com.coliving.api.messaging.application.usecase.ConversationService
import com.coliving.api.messaging.domain.model.Message
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConversationServiceTest {

    private val conversations = FakeConversationRepository()
    private val messages = FakeMessageRepository()
    private val parties = FakeReservationPartiesPort()
    private val service = ConversationService(conversations, messages, parties)

    private val guestId = UUID.randomUUID()
    private val hostId = UUID.randomUUID()

    private fun reservation(): UUID {
        val reservationId = UUID.randomUUID()
        parties.add(reservationId = reservationId, guestUserId = guestId, hostUserId = hostId)
        return reservationId
    }

    @Test
    fun `guest and host land on the same thread (RF-050)`() {
        val reservationId = reservation()

        val byGuest = service.open(OpenConversationCommand(reservationId, guestId))
        val byHost = service.open(OpenConversationCommand(reservationId, hostId))

        assertEquals(byGuest.conversation.id, byHost.conversation.id)
        assertEquals(1, conversations.store.size)
        assertEquals(guestId, byGuest.conversation.guestUserId)
        assertEquals(hostId, byGuest.conversation.hostUserId)
    }

    @Test
    fun `the detail read carries the stay dates and the messages (RF-050)`() {
        val reservationId = reservation()
        val detail = service.open(OpenConversationCommand(reservationId, guestId))

        assertEquals(reservationId, detail.stay.reservationId)
        assertEquals(parties.parties.getValue(reservationId).fromDate, detail.stay.fromDate)

        // Stored out of order on purpose: the read sorts by send time.
        val base = Instant.parse("2026-09-08T14:00:00Z")
        messages.save(Message(UUID.randomUUID(), detail.conversation.id, guestId, "segundo", base.plusSeconds(60)))
        messages.save(Message(UUID.randomUUID(), detail.conversation.id, hostId, "primero", base))

        val refreshed = service.get(detail.conversation.id, hostId)

        assertEquals(listOf("primero", "segundo"), refreshed.messages.map { it.body })
        assertEquals(listOf(hostId, guestId), refreshed.messages.map { it.senderUserId })
        assertEquals(2, refreshed.conversation.messageCount)
        assertEquals(base.plusSeconds(60), refreshed.conversation.lastMessageAt)
    }

    @Test
    fun `threads are listed for both sides only (RF-050)`() {
        val reservationId = reservation()
        service.open(OpenConversationCommand(reservationId, guestId))

        assertEquals(1, service.listMine(guestId).size)
        assertEquals(1, service.listMine(hostId).size)
        assertTrue { service.listMine(UUID.randomUUID()).isEmpty() }
    }

    @Test
    fun `rejects opening a conversation of an unknown reservation`() {
        assertFailsWith<NotFoundException> {
            service.open(OpenConversationCommand(UUID.randomUUID(), guestId))
        }
    }

    @Test
    fun `rejects users outside the reservation`() {
        val reservationId = reservation()

        assertFailsWith<ForbiddenException> {
            service.open(OpenConversationCommand(reservationId, UUID.randomUUID()))
        }
        assertEquals(0, conversations.store.size)
    }

    @Test
    fun `rejects reading a thread the user is not part of`() {
        val reservationId = reservation()
        val detail = service.open(OpenConversationCommand(reservationId, guestId))

        assertFailsWith<ForbiddenException> { service.get(detail.conversation.id, UUID.randomUUID()) }
        assertFailsWith<NotFoundException> { service.get(UUID.randomUUID(), guestId) }
    }
}
