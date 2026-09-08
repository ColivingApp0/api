package com.coliving.api.messaging.infrastructure.persistence.entity

import com.coliving.api.messaging.domain.enums.NotificationType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "messaging_conversation")
class ConversationEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    // Cross-context reference to the reservation: plain UUID (no FK by architecture rule).
    @Column(name = "reservation_id", nullable = false, unique = true)
    var reservationId: UUID,

    @Column(name = "guest_user_id", nullable = false)
    var guestUserId: UUID,

    @Column(name = "host_user_id", nullable = false)
    var hostUserId: UUID,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
)

@Entity
@Table(name = "messaging_message")
class MessageEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "conversation_id", nullable = false)
    var conversationId: UUID,

    @Column(name = "sender_user_id", nullable = false)
    var senderUserId: UUID,

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    var body: String,

    @Column(name = "sent_at", nullable = false)
    var sentAt: Instant,
)

@Entity
@Table(name = "messaging_notification")
class NotificationEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    var type: NotificationType,

    @Column(name = "title", nullable = false, length = 200)
    var title: String,

    // Points at the conversation today; the column is generic on purpose (RF-051).
    @Column(name = "reference_id", nullable = false)
    var referenceId: UUID,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "read_at")
    var readAt: Instant?,
)