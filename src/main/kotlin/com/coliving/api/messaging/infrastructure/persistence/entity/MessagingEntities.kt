package com.coliving.api.messaging.infrastructure.persistence.entity

import com.coliving.api.messaging.domain.enums.NotificationType
import com.coliving.api.messaging.domain.enums.PushPlatform
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "messaging_conversation")
class ConversationEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    // Cross-context references are plain UUIDs (no FK by architecture rule).
    @Column(name = "reservation_id", unique = true)
    var reservationId: UUID?,

    @Column(name = "unit_id")
    var unitId: UUID?,

    @Column(name = "publication_id")
    var publicationId: UUID?,

    @Column(name = "subject", length = 255)
    var subject: String?,

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

@Entity
@Table(
    name = "messaging_conversation_block",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_messaging_block_pair", columnNames = ["blocker_user_id", "blocked_user_id"]),
    ],
)
class ConversationBlockEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "blocker_user_id", nullable = false)
    var blockerUserId: UUID,

    @Column(name = "blocked_user_id", nullable = false)
    var blockedUserId: UUID,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
)

@Entity
@Table(
    name = "messaging_conversation_report",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_messaging_report_pair", columnNames = ["conversation_id", "reporter_user_id"]),
    ],
)
class ConversationReportEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "conversation_id", nullable = false)
    var conversationId: UUID,

    @Column(name = "reporter_user_id", nullable = false)
    var reporterUserId: UUID,

    @Column(name = "reported_user_id", nullable = false)
    var reportedUserId: UUID,

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    var reason: String,

    // Reference to the moderation case raised with the report (no FK).
    @Column(name = "case_id", nullable = false)
    var caseId: UUID,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
)

@Entity
@Table(
    name = "messaging_push_token",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_messaging_push_token_token", columnNames = ["token"]),
    ],
)
class PushTokenEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "token", nullable = false, length = 512)
    var token: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    var platform: PushPlatform,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "last_seen_at", nullable = false)
    var lastSeenAt: Instant,
)