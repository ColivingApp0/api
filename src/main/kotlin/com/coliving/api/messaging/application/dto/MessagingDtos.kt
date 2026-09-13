package com.coliving.api.messaging.application.dto

import com.coliving.api.messaging.domain.enums.NotificationType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** A message as returned to clients. The sender is always identified (RF-050). */
data class MessageView(
    val id: UUID,
    val conversationId: UUID,
    val senderUserId: UUID,
    val body: String,
    val sentAt: Instant,
)

/**
 * Thread summary: the reservation it belongs to, both parties and its last
 * activity. It carries no stay dates on purpose — listing the threads of a user
 * must not reach into `booking`; the detail read adds them as
 * [ReservationStayView].
 */
data class ConversationView(
    val id: UUID,
    val reservationId: UUID,
    val guestUserId: UUID,
    val hostUserId: UUID,
    val createdAt: Instant,
    val lastMessageAt: Instant?,
    val messageCount: Int,
)

/** Stay header of a thread, resolved from `booking` on the detail read (RF-050). */
data class ReservationStayView(
    val reservationId: UUID,
    val fromDate: LocalDate,
    val toDate: LocalDate,
)

/** Conversation plus its messages in chronological order (RF-050). */
data class ConversationDetailView(
    val conversation: ConversationView,
    val stay: ReservationStayView,
    val messages: List<MessageView>,
)

/** Command to open (or reopen) the conversation attached to a reservation. */
data class OpenConversationCommand(
    val reservationId: UUID,
    val actorId: UUID,
)

/** Command to post a message inside a conversation. */
data class SendMessageCommand(
    val conversationId: UUID,
    val senderUserId: UUID,
    val body: String,
)

/** Command to post a message straight into a reservation's conversation. */
data class SendMessageToReservationCommand(
    val reservationId: UUID,
    val senderUserId: UUID,
    val body: String,
)

/** In-app notification of the authenticated user (RF-051). */
data class NotificationView(
    val id: UUID,
    val type: NotificationType,
    val title: String,
    val referenceId: UUID,
    val createdAt: Instant,
    val readAt: Instant?,
    val unread: Boolean,
)

/** User inbox plus the unread counter that feeds the app badge (RF-051). */
data class NotificationInboxView(
    val unreadCount: Int,
    val notifications: List<NotificationView>,
)

/** A block raised by the authenticated user (RF-053). */
data class BlockView(
    val blockedUserId: UUID,
    val createdAt: Instant,
)

/** Report of a conversation with its moderation case reference (RF-052). */
data class ConversationReportView(
    val id: UUID,
    val conversationId: UUID,
    val reportedUserId: UUID,
    val reason: String,
    val caseId: UUID,
    val createdAt: Instant,
)

/** Command to report a conversation (RF-052). */
data class ReportConversationCommand(
    val conversationId: UUID,
    val reporterUserId: UUID,
    val reason: String,
)

