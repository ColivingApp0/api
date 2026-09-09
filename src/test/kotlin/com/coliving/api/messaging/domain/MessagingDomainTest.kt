package com.coliving.api.messaging.domain

import com.coliving.api.messaging.domain.model.Conversation
import com.coliving.api.messaging.domain.model.Message
import com.coliving.api.messaging.domain.model.Notification
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.InvalidArgumentException
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Invariants of the messaging aggregates (RF-050, RF-051). */
class MessagingDomainTest {

    private val guestId = UUID.randomUUID()
    private val hostId = UUID.randomUUID()
    private val now = Instant.parse("2026-09-08T14:00:00Z")

    private fun conversation(): Conversation =
        Conversation.start(
            id = UUID.randomUUID(),
            reservationId = UUID.randomUUID(),
            guestUserId = guestId,
            hostUserId = hostId,
            now = now,
        )

    @Test
    fun `a conversation needs two distinct parties`() {
        assertFailsWith<ConflictException> {
            Conversation.start(UUID.randomUUID(), UUID.randomUUID(), guestId, guestId, now)
        }
    }

    @Test
    fun `only the two parties are participants and each sees the other as counterpart`() {
        val conversation = conversation()
        val outsider = UUID.randomUUID()

        assertTrue(conversation.isParticipant(guestId))
        assertTrue(conversation.isParticipant(hostId))
        assertFalse(conversation.isParticipant(outsider))
        assertEquals(hostId, conversation.counterpartOf(guestId))
        assertEquals(guestId, conversation.counterpartOf(hostId))
        assertFailsWith<ForbiddenException> { conversation.counterpartOf(outsider) }
        assertFailsWith<ForbiddenException> { conversation.requireParticipant(outsider) }
    }

    @Test
    fun `messages are trimmed, limited and signed by an actual participant`() {
        val conversation = conversation()

        val message = Message.send(UUID.randomUUID(), conversation, hostId, "  disponible  ", now)

        assertEquals("disponible", message.body)
        assertEquals(hostId, message.senderUserId)
        assertEquals(now, message.sentAt)

        assertFailsWith<InvalidArgumentException> {
            Message.send(UUID.randomUUID(), conversation, hostId, "   ", now)
        }
        assertFailsWith<InvalidArgumentException> {
            Message.send(
                UUID.randomUUID(),
                conversation,
                hostId,
                "x".repeat(Message.MAX_BODY_LENGTH + 1),
                now,
            )
        }
        assertFailsWith<ForbiddenException> {
            Message.send(UUID.randomUUID(), conversation, UUID.randomUUID(), "hola", now)
        }
    }

    @Test
    fun `a new message notification is content-free and starts unread`() {
        val conversationId = UUID.randomUUID()

        val notification = Notification.forNewMessage(UUID.randomUUID(), hostId, conversationId, now)

        assertEquals(hostId, notification.userId)
        assertEquals(conversationId, notification.referenceId)
        assertEquals(Notification.NEW_MESSAGE_TITLE, notification.title)
        assertNull(notification.readAt)
        assertTrue(notification.isUnread())
        // The label never leaks the message body.
        assertFalse(notification.title.contains(conversationId.toString()))
    }

    @Test
    fun `markRead is idempotent and only the owner may read it`() {
        val notification = Notification.forNewMessage(UUID.randomUUID(), hostId, UUID.randomUUID(), now)
        val readAt = now.plusSeconds(600)

        val read = notification.markRead(readAt)

        assertFalse(read.isUnread())
        assertEquals(readAt, read.readAt)
        // Already read: the first read moment is preserved.
        assertEquals(readAt, read.markRead(now.plusSeconds(900)).readAt)
        assertFailsWith<ForbiddenException> { notification.requireOwner(guestId) }
        notification.requireOwner(hostId)
    }
}