package com.coliving.api.messaging.application

import com.coliving.api.messaging.application.dto.OpenConversationCommand
import com.coliving.api.messaging.application.dto.SendMessageCommand
import com.coliving.api.messaging.application.dto.SendMessageToReservationCommand
import com.coliving.api.messaging.application.usecase.ConversationService
import com.coliving.api.messaging.application.usecase.MessageService
import com.coliving.api.messaging.domain.enums.NotificationType
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.InvalidArgumentException
import com.coliving.api.shared.error.NotFoundException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MessageServiceTest {

    private val conversations = FakeConversationRepository()
    private val messages = FakeMessageRepository()
    private val notifications = FakeNotificationRepository()
    private val blocks = FakeUserBlockRepository()
    private val parties = FakeReservationPartiesPort()
    private val conversationService = ConversationService(conversations, messages, parties)
    private val service = MessageService(conversations, messages, notifications, conversationService, blocks)

    private val guestId = UUID.randomUUID()
    private val hostId = UUID.randomUUID()

    private fun openConversation(): UUID {
        val reservationId = UUID.randomUUID()
        parties.add(reservationId = reservationId, guestUserId = guestId, hostUserId = hostId)
        return conversationService.open(OpenConversationCommand(reservationId, guestId)).conversation.id
    }

    @Test
    fun `stores the message identified by its sender (RF-050)`() {
        val conversationId = openConversation()

        val view = service.send(SendMessageCommand(conversationId, guestId, "  Hola, ¿está libre?  "))

        assertEquals("Hola, ¿está libre?", view.body)
        assertEquals(guestId, view.senderUserId)
        assertEquals(conversationId, view.conversationId)
        assertEquals(1, messages.findByConversation(conversationId).size)
    }

    @Test
    fun `notifies the counterpart of the sender (RF-051)`() {
        val conversationId = openConversation()

        service.send(SendMessageCommand(conversationId, guestId, "primero"))

        val hostInbox = notifications.findByUser(hostId)
        assertEquals(1, hostInbox.size)
        assertEquals(NotificationType.MENSAJE_NUEVO, hostInbox.single().type)
        assertEquals(conversationId, hostInbox.single().referenceId)
        // The sender is never notified of their own message.
        assertEquals(0, notifications.findByUser(guestId).size)

        service.send(SendMessageCommand(conversationId, hostId, "respuesta"))
        assertEquals(1, notifications.findByUser(guestId).size)
    }

    @Test
    fun `rejects writers outside the conversation`() {
        val conversationId = openConversation()

        assertFailsWith<ForbiddenException> {
            service.send(SendMessageCommand(conversationId, UUID.randomUUID(), "no soy parte"))
        }
        assertEquals(0, messages.store.size)
    }

    @Test
    fun `rejects empty and oversized bodies`() {
        val conversationId = openConversation()

        assertFailsWith<InvalidArgumentException> {
            service.send(SendMessageCommand(conversationId, guestId, "   "))
        }
        assertFailsWith<InvalidArgumentException> {
            service.send(SendMessageCommand(conversationId, guestId, "x".repeat(2001)))
        }
        assertEquals(0, messages.store.size)
    }

    @Test
    fun `rejects messages on an unknown conversation`() {
        assertFailsWith<NotFoundException> {
            service.send(SendMessageCommand(UUID.randomUUID(), guestId, "hola"))
        }
    }

    @Test
    fun `addressing the reservation opens the thread on demand (RF-050)`() {
        val reservationId = UUID.randomUUID()
        parties.add(reservationId = reservationId, guestUserId = guestId, hostUserId = hostId)

        val view = service.sendToReservation(
            SendMessageToReservationCommand(reservationId, hostId, "Bienvenido"),
        )

        assertEquals(hostId, view.senderUserId)
        assertEquals(1, conversations.store.size)
        val conversation = conversations.findByReservation(reservationId)!!
        assertEquals(conversation.id, view.conversationId)
        assertEquals(1, notifications.findByUser(guestId).size)
    }
}
