package com.coliving.api.messaging.domain.repository

import com.coliving.api.messaging.domain.model.Conversation
import com.coliving.api.messaging.domain.model.ConversationReport
import com.coliving.api.messaging.domain.model.Message
import com.coliving.api.messaging.domain.model.Notification
import com.coliving.api.messaging.domain.model.UserBlock
import java.util.UUID

interface ConversationRepository {
    fun findById(id: UUID): Conversation?

    /** The single conversation of a reservation, if it was already opened. */
    fun findByReservation(reservationId: UUID): Conversation?

    /** Conversations where the user is guest or host, newest first. */
    fun findByParticipant(userId: UUID): List<Conversation>

    fun save(conversation: Conversation)
}

interface MessageRepository {
    /** Messages of a conversation ordered by send time (RF-050). */
    fun findByConversation(conversationId: UUID): List<Message>

    /** Number of messages of a conversation, for the thread summary. */
    fun countByConversation(conversationId: UUID): Int

    /** Most recent message of a conversation, or null when it has none. */
    fun findLastByConversation(conversationId: UUID): Message?

    fun save(message: Message)
}

interface NotificationRepository {
    fun findById(id: UUID): Notification?

    /** Notifications of a user, newest first. */
    fun findByUser(userId: UUID): List<Notification>

    /** Unread notifications of a user, newest first. */
    fun findUnreadByUser(userId: UUID): List<Notification>

    fun save(notification: Notification)
}

/** Blocks raised by a user (RF-053). */
interface UserBlockRepository {

    /** Whether the block exists in either direction between the two users. */
    fun existsBetween(userA: UUID, userB: UUID): Boolean

    /** Blocks raised by [blockerUserId], newest first. */
    fun findByBlocker(blockerUserId: UUID): List<UserBlock>

    fun findByBlockerAndBlocked(blockerUserId: UUID, blockedUserId: UUID): UserBlock?

    fun save(block: UserBlock)

    fun delete(block: UserBlock)
}

/** Conversation reports (RF-052), with their moderation case reference. */
interface ConversationReportRepository {

    /** Reports raised by a user, newest first. */
    fun findByReporter(reporterUserId: UUID): List<ConversationReport>

    fun findByConversationAndReporter(conversationId: UUID, reporterUserId: UUID): ConversationReport?

    fun save(report: ConversationReport)
}
