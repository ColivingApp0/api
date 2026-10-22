package com.coliving.api.messaging.application.dto

import com.coliving.api.messaging.domain.enums.NotificationType
import java.time.Instant
import java.util.UUID

/** A message as returned to clients. The sender is always identified (RF-050). */
data class MessageView(
    val id: UUID,
    val conversationId: UUID,
    val senderUserId: UUID,
    val body: String,
    val sentAt: Instant,
)

/** Thread summary with its listing/reservation context. */
data class ConversationView(
    val id: UUID,
    val reservationId: UUID?,
    val unitId: UUID?,
    val publicationId: UUID?,
    val subject: String?,
    val guestUserId: UUID,
    val hostUserId: UUID,
    val createdAt: Instant,
    val lastMessageAt: Instant?,
    val messageCount: Int,
    val roomTitle: String? = null,
    val propertyTitle: String? = null,
    val cityName: String? = null,
)

/** Room header of a thread, resolved from accommodation on the read. */
data class ListingContextView(
    val unitId: UUID?,
    val publicationId: UUID?,
    val roomTitle: String?,
    val propertyTitle: String?,
    val cityName: String?,
    val reservationId: UUID?,
)

/** Conversation plus its messages in chronological order (RF-050). */
data class ConversationDetailView(
    val conversation: ConversationView,
    val listing: ListingContextView,
    val messages: List<MessageView>,
)

/** Opens (or reopens) a conversation about a reservation or a room listing. */
data class OpenConversationCommand(
    val actorId: UUID,
    val reservationId: UUID? = null,
    val unitId: UUID? = null,
)

data class SendMessageCommand(
    val conversationId: UUID,
    val senderUserId: UUID,
    val body: String,
)

data class SendMessageToReservationCommand(
    val reservationId: UUID,
    val senderUserId: UUID,
    val body: String,
)

data class NotificationView(
    val id: UUID,
    val type: NotificationType,
    val title: String,
    val referenceId: UUID,
    val createdAt: Instant,
    val readAt: Instant?,
    val unread: Boolean,
)

data class NotificationInboxView(
    val unreadCount: Int,
    val notifications: List<NotificationView>,
)

data class BlockView(
    val blockedUserId: UUID,
    val createdAt: Instant,
)

data class ConversationReportView(
    val id: UUID,
    val conversationId: UUID,
    val reportedUserId: UUID,
    val reason: String,
    val caseId: UUID,
    val createdAt: Instant,
)

data class ReportConversationCommand(
    val conversationId: UUID,
    val reporterUserId: UUID,
    val reason: String,
)
