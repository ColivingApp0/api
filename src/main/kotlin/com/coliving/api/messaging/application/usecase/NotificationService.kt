package com.coliving.api.messaging.application.usecase

import com.coliving.api.messaging.application.dto.NotificationInboxView
import com.coliving.api.messaging.application.dto.NotificationView
import com.coliving.api.messaging.domain.model.Notification
import com.coliving.api.messaging.domain.repository.NotificationRepository
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Notification center of the authenticated user (RF-051): the inbox with its
 * unread counter and the per-notification read mark. Only the owner can read or
 * acknowledge a notification, which [Notification.requireOwner] enforces.
 */
@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
) {

    @Transactional(readOnly = true)
    fun inbox(userId: UUID): NotificationInboxView {
        val notifications = notificationRepository.findByUser(userId)
        return NotificationInboxView(
            unreadCount = notificationRepository.findUnreadByUser(userId).size,
            notifications = notifications.map { it.toView() },
        )
    }

    @Transactional
    fun markRead(notificationId: UUID, userId: UUID): NotificationView {
        val notification = notificationRepository.findById(notificationId)
            ?: throw NotFoundException("Notification not found")
        notification.requireOwner(userId)
        val read = notification.markRead(Instant.now())
        if (read !== notification) {
            notificationRepository.save(read)
        }
        return read.toView()
    }
}

/** Shared notification projection. */
internal fun Notification.toView(): NotificationView =
    NotificationView(
        id = id,
        type = type,
        title = title,
        referenceId = referenceId,
        createdAt = createdAt,
        readAt = readAt,
        unread = isUnread(),
    )