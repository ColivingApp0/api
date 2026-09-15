package com.coliving.api.messaging.infrastructure.persistence.mapper

import com.coliving.api.messaging.domain.model.Conversation
import com.coliving.api.messaging.domain.model.ConversationReport
import com.coliving.api.messaging.domain.model.Message
import com.coliving.api.messaging.domain.model.Notification
import com.coliving.api.messaging.domain.model.UserBlock
import com.coliving.api.messaging.infrastructure.persistence.entity.ConversationBlockEntity
import com.coliving.api.messaging.infrastructure.persistence.entity.ConversationEntity
import com.coliving.api.messaging.infrastructure.persistence.entity.ConversationReportEntity
import com.coliving.api.messaging.infrastructure.persistence.entity.MessageEntity
import com.coliving.api.messaging.infrastructure.persistence.entity.NotificationEntity

/**
 * Pure domain <-> entity mapping, mirroring booking's mappers: models are plain
 * classes built through their factories, so persisted state never bypasses a
 * domain rule.
 */
object MessagingMappers {

    fun toDomain(entity: ConversationEntity): Conversation =
        Conversation(
            id = entity.id,
            reservationId = entity.reservationId,
            guestUserId = entity.guestUserId,
            hostUserId = entity.hostUserId,
            createdAt = entity.createdAt,
        )

    fun toEntity(conversation: Conversation): ConversationEntity =
        ConversationEntity(
            id = conversation.id,
            reservationId = conversation.reservationId,
            guestUserId = conversation.guestUserId,
            hostUserId = conversation.hostUserId,
            createdAt = conversation.createdAt,
        )

    fun toDomain(entity: MessageEntity): Message =
        Message(
            id = entity.id,
            conversationId = entity.conversationId,
            senderUserId = entity.senderUserId,
            body = entity.body,
            sentAt = entity.sentAt,
        )

    fun toEntity(message: Message): MessageEntity =
        MessageEntity(
            id = message.id,
            conversationId = message.conversationId,
            senderUserId = message.senderUserId,
            body = message.body,
            sentAt = message.sentAt,
        )

    fun toDomain(entity: NotificationEntity): Notification =
        Notification(
            id = entity.id,
            userId = entity.userId,
            type = entity.type,
            title = entity.title,
            referenceId = entity.referenceId,
            createdAt = entity.createdAt,
            readAt = entity.readAt,
        )

    fun toEntity(notification: Notification): NotificationEntity =
        NotificationEntity(
            id = notification.id,
            userId = notification.userId,
            type = notification.type,
            title = notification.title,
            referenceId = notification.referenceId,
            createdAt = notification.createdAt,
            readAt = notification.readAt,
        )

    fun toDomain(entity: ConversationBlockEntity): UserBlock =
        UserBlock(
            blockerUserId = entity.blockerUserId,
            blockedUserId = entity.blockedUserId,
            createdAt = entity.createdAt,
        )

    fun toEntity(block: UserBlock): ConversationBlockEntity =
        ConversationBlockEntity(
            blockerUserId = block.blockerUserId,
            blockedUserId = block.blockedUserId,
            createdAt = block.createdAt,
        )

    fun toBlockList(entities: List<ConversationBlockEntity>): List<UserBlock> =
        entities.map { toDomain(it) }

    fun toDomain(entity: ConversationReportEntity): ConversationReport =
        ConversationReport(
            id = entity.id,
            conversationId = entity.conversationId,
            reporterUserId = entity.reporterUserId,
            reportedUserId = entity.reportedUserId,
            reason = entity.reason,
            caseId = entity.caseId,
            createdAt = entity.createdAt,
        )

    fun toEntity(report: ConversationReport): ConversationReportEntity =
        ConversationReportEntity(
            id = report.id,
            conversationId = report.conversationId,
            reporterUserId = report.reporterUserId,
            reportedUserId = report.reportedUserId,
            reason = report.reason,
            caseId = report.caseId,
            createdAt = report.createdAt,
        )

    fun toReportList(entities: List<ConversationReportEntity>): List<ConversationReport> =
        entities.map { toDomain(it) }
}