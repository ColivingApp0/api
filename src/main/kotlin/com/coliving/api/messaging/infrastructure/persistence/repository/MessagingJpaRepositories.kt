package com.coliving.api.messaging.infrastructure.persistence.repository

import com.coliving.api.messaging.infrastructure.persistence.entity.ConversationEntity
import com.coliving.api.messaging.infrastructure.persistence.entity.MessageEntity
import com.coliving.api.messaging.infrastructure.persistence.entity.NotificationEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface ConversationJpaRepository : JpaRepository<ConversationEntity, UUID> {

    fun findByReservationId(reservationId: UUID): ConversationEntity?

    /** Threads of a user, whichever side they are on; newest first. */
    fun findByGuestUserIdOrHostUserIdOrderByCreatedAtDesc(
        guestUserId: UUID,
        hostUserId: UUID,
    ): List<ConversationEntity>
}

interface MessageJpaRepository : JpaRepository<MessageEntity, UUID> {

    /** Chronological reading order (RF-050). */
    fun findByConversationIdOrderBySentAtAsc(conversationId: UUID): List<MessageEntity>

    /** Latest message of a thread, for the summary's last-activity mark. */
    fun findFirstByConversationIdOrderBySentAtDesc(conversationId: UUID): MessageEntity?

    fun countByConversationId(conversationId: UUID): Long
}

interface NotificationJpaRepository : JpaRepository<NotificationEntity, UUID> {

    /** Inbox of a user, newest first. */
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<NotificationEntity>

    /** Unread notifications of a user, newest first. */
    fun findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId: UUID): List<NotificationEntity>
}