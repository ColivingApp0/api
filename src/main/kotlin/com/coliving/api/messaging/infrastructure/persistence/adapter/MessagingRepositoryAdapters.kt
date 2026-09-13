package com.coliving.api.messaging.infrastructure.persistence.adapter

import com.coliving.api.messaging.domain.model.Conversation
import com.coliving.api.messaging.domain.model.ConversationReport
import com.coliving.api.messaging.domain.model.Message
import com.coliving.api.messaging.domain.model.Notification
import com.coliving.api.messaging.domain.model.UserBlock
import com.coliving.api.messaging.domain.repository.ConversationRepository
import com.coliving.api.messaging.domain.repository.ConversationReportRepository
import com.coliving.api.messaging.domain.repository.MessageRepository
import com.coliving.api.messaging.domain.repository.NotificationRepository
import com.coliving.api.messaging.domain.repository.UserBlockRepository
import com.coliving.api.messaging.infrastructure.persistence.entity.ConversationBlockEntity
import com.coliving.api.messaging.infrastructure.persistence.entity.ConversationReportEntity
import com.coliving.api.messaging.infrastructure.persistence.mapper.MessagingMappers
import com.coliving.api.messaging.infrastructure.persistence.repository.ConversationBlockJpaRepository
import com.coliving.api.messaging.infrastructure.persistence.repository.ConversationJpaRepository
import com.coliving.api.messaging.infrastructure.persistence.repository.ConversationReportJpaRepository
import com.coliving.api.messaging.infrastructure.persistence.repository.MessageJpaRepository
import com.coliving.api.messaging.infrastructure.persistence.repository.NotificationJpaRepository
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class ConversationRepositoryAdapter(
    private val jpaRepository: ConversationJpaRepository,
) : ConversationRepository {

    override fun findById(id: UUID): Conversation? =
        jpaRepository.findById(id).map(MessagingMappers::toDomain).orElse(null)

    override fun findByReservation(reservationId: UUID): Conversation? =
        jpaRepository.findByReservationId(reservationId)?.let(MessagingMappers::toDomain)

    override fun findByParticipant(userId: UUID): List<Conversation> =
        jpaRepository.findByGuestUserIdOrHostUserIdOrderByCreatedAtDesc(userId, userId)
            .map(MessagingMappers::toDomain)

    @Transactional(propagation = Propagation.MANDATORY)
    override fun save(conversation: Conversation) {
        jpaRepository.save(MessagingMappers.toEntity(conversation))
    }
}

@Component
class MessageRepositoryAdapter(
    private val jpaRepository: MessageJpaRepository,
) : MessageRepository {

    override fun findByConversation(conversationId: UUID): List<Message> =
        jpaRepository.findByConversationIdOrderBySentAtAsc(conversationId)
            .map(MessagingMappers::toDomain)

    override fun countByConversation(conversationId: UUID): Int =
        jpaRepository.countByConversationId(conversationId).toInt()

    override fun findLastByConversation(conversationId: UUID): Message? =
        jpaRepository.findFirstByConversationIdOrderBySentAtDesc(conversationId)
            ?.let(MessagingMappers::toDomain)

    @Transactional(propagation = Propagation.MANDATORY)
    override fun save(message: Message) {
        jpaRepository.save(MessagingMappers.toEntity(message))
    }
}

@Component
class NotificationRepositoryAdapter(
    private val jpaRepository: NotificationJpaRepository,
) : NotificationRepository {

    override fun findById(id: UUID): Notification? =
        jpaRepository.findById(id).map(MessagingMappers::toDomain).orElse(null)

    override fun findByUser(userId: UUID): List<Notification> =
        jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).map(MessagingMappers::toDomain)

    override fun findUnreadByUser(userId: UUID): List<Notification> =
        jpaRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId)
            .map(MessagingMappers::toDomain)

    @Transactional(propagation = Propagation.MANDATORY)
    override fun save(notification: Notification) {
        jpaRepository.save(MessagingMappers.toEntity(notification))
    }
}

@Component
class UserBlockRepositoryAdapter(
    private val jpaRepository: ConversationBlockJpaRepository,
) : UserBlockRepository {

    override fun existsBetween(userA: UUID, userB: UUID): Boolean =
        jpaRepository.existsByBlockerUserIdAndBlockedUserId(userA, userB) ||
            jpaRepository.existsByBlockerUserIdAndBlockedUserId(userB, userA)

    override fun findByBlocker(blockerUserId: UUID): List<UserBlock> =
        MessagingMappers.toBlockList(jpaRepository.findByBlockerUserIdOrderByCreatedAtDesc(blockerUserId))

    override fun findByBlockerAndBlocked(blockerUserId: UUID, blockedUserId: UUID): UserBlock? =
        jpaRepository.findByBlockerUserIdAndBlockedUserId(blockerUserId, blockedUserId)
            ?.let(MessagingMappers::toDomain)

    @Transactional(propagation = Propagation.MANDATORY)
    override fun save(block: UserBlock) {
        jpaRepository.save(MessagingMappers.toEntity(block))
    }

    override fun delete(block: UserBlock) {
        jpaRepository.delete(MessagingMappers.toEntity(block))
    }
}

@Component
class ConversationReportRepositoryAdapter(
    private val jpaRepository: ConversationReportJpaRepository,
) : ConversationReportRepository {

    override fun findByReporter(reporterUserId: UUID): List<ConversationReport> =
        MessagingMappers.toReportList(jpaRepository.findByReporterUserIdOrderByCreatedAtDesc(reporterUserId))

    override fun findByConversationAndReporter(
        conversationId: UUID,
        reporterUserId: UUID,
    ): ConversationReport? =
        jpaRepository.findByConversationIdAndReporterUserId(conversationId, reporterUserId)
            ?.let(MessagingMappers::toDomain)

    @Transactional(propagation = Propagation.MANDATORY)
    override fun save(report: ConversationReport) {
        jpaRepository.save(MessagingMappers.toEntity(report))
    }
}