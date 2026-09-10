package com.coliving.api.messaging.domain.model

import com.coliving.api.messaging.domain.enums.NotificationType
import com.coliving.api.shared.error.ForbiddenException
import java.time.Instant
import java.util.UUID

/**
 * In-app notification of a user (RF-051). The payload is intentionally
 * content-free: [title] is a generic label and [referenceId] points at the
 * related object (the conversation or, later, a reservation). Neither the
 * message body nor reservation details are stored here, so nothing sensitive
 * can leak to a locked-screen preview.
 */
class Notification(
    val id: UUID,
    val userId: UUID,
    val type: NotificationType,
    val title: String,
    val referenceId: UUID,
    val createdAt: Instant,
    val readAt: Instant?,
) {

    /** Whether this notification is still unread. */
    fun isUnread(): Boolean = readAt == null

    /** Marks the notification as read, keeping the first read moment. */
    fun markRead(now: Instant): Notification =
        if (readAt != null) this else copyWithReadAt(now)

    /** Fails unless [userId] owns this notification. */
    fun requireOwner(userId: UUID) {
        if (this.userId != userId) {
            throw ForbiddenException("Not the owner of this notification")
        }
    }

    private fun copyWithReadAt(now: Instant): Notification =
        Notification(
            id = id,
            userId = userId,
            type = type,
            title = title,
            referenceId = referenceId,
            createdAt = createdAt,
            readAt = now,
        )

    companion object {
        /** Safe, content-free label shown for a new message (RF-051). */
        const val NEW_MESSAGE_TITLE = "Tienes un nuevo mensaje"

        fun forNewMessage(
            id: UUID,
            userId: UUID,
            conversationId: UUID,
            now: Instant,
        ): Notification = Notification(
            id = id,
            userId = userId,
            type = NotificationType.MENSAJE_NUEVO,
            title = NEW_MESSAGE_TITLE,
            referenceId = conversationId,
            createdAt = now,
            readAt = null,
        )
    }
}
