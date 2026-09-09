package com.coliving.api.messaging.application

import com.coliving.api.messaging.application.usecase.NotificationService
import com.coliving.api.messaging.domain.model.Notification
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NotificationServiceTest {

    private val notifications = FakeNotificationRepository()
    private val service = NotificationService(notifications)

    private val userId = UUID.randomUUID()

    private fun store(userId: UUID = this.userId, createdAt: Instant = Instant.now()): Notification {
        val notification = Notification.forNewMessage(
            id = UUID.randomUUID(),
            userId = userId,
            conversationId = UUID.randomUUID(),
            now = createdAt,
        )
        notifications.save(notification)
        return notification
    }

    @Test
    fun `inbox lists the notifications of the owner with the unread counter (RF-051)`() {
        val first = store(createdAt = Instant.parse("2026-09-08T13:00:00Z"))
        store(createdAt = Instant.parse("2026-09-08T15:00:00Z"))
        store(userId = UUID.randomUUID())

        val inbox = service.inbox(userId)

        assertEquals(2, inbox.notifications.size)
        assertEquals(2, inbox.unreadCount)
        assertEquals(Notification.NEW_MESSAGE_TITLE, inbox.notifications.first().title)
        assertEquals(first.referenceId, inbox.notifications.last().referenceId)
    }

    @Test
    fun `markRead clears the unread flag and keeps the first read moment`() {
        val notification = store()

        val first = service.markRead(notification.id, userId)
        val again = service.markRead(notification.id, userId)

        assertFalse(first.unread)
        assertNotNull(first.readAt)
        assertEquals(first.readAt, again.readAt)
        assertEquals(0, service.inbox(userId).unreadCount)
    }

    @Test
    fun `rejects reading another user's notification`() {
        val notification = store()

        assertFailsWith<ForbiddenException> { service.markRead(notification.id, UUID.randomUUID()) }
    }

    @Test
    fun `unknown notifications are not found`() {
        assertFailsWith<NotFoundException> { service.markRead(UUID.randomUUID(), userId) }
        assertTrue { service.inbox(UUID.randomUUID()).notifications.isEmpty() }
    }
}