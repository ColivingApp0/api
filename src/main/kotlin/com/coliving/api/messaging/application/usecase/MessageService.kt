package com.coliving.api.messaging.application.usecase

import com.coliving.api.messaging.application.dto.MessageView
import com.coliving.api.messaging.application.dto.OpenConversationCommand
import com.coliving.api.messaging.application.dto.SendMessageCommand
import com.coliving.api.messaging.application.dto.SendMessageToReservationCommand
import com.coliving.api.messaging.domain.model.Conversation
import com.coliving.api.messaging.domain.model.Message
import com.coliving.api.messaging.domain.model.Notification
import com.coliving.api.messaging.domain.repository.ConversationRepository
import com.coliving.api.messaging.domain.repository.MessageRepository
import com.coliving.api.messaging.domain.repository.NotificationRepository
import com.coliving.api.messaging.domain.repository.UserBlockRepository
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Writing side of a reservation conversation (RF-050). Every accepted message
 * is persisted with its sender and, in the same transaction, raises the in-app
 * notification of the counterpart (RF-051) — the receiver never has to poll the
 * conversation to notice new activity.
 */
@Service
class MessageService(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val notificationRepository: NotificationRepository,
    private val conversationService: ConversationService,
    private val userBlockRepository: UserBlockRepository,
) {

    @Transactional
    fun send(command: SendMessageCommand): MessageView {
        val conversation = conversationRepository.findById(command.conversationId)
            ?: throw NotFoundException("Conversation not found")
        return persist(conversation, command.senderUserId, command.body)
    }

    /**
     * Convenience for clients that address the reservation instead of the
     * conversation: the thread is opened on demand for the sender.
     */
    @Transactional
    fun sendToReservation(command: SendMessageToReservationCommand): MessageView {
        conversationService.open(
            OpenConversationCommand(
                reservationId = command.reservationId,
                actorId = command.senderUserId,
            ),
        )
        val conversation = conversationRepository.findByReservation(command.reservationId)
            ?: throw NotFoundException("Conversation not found")
        return persist(conversation, command.senderUserId, command.body)
    }

    private fun persist(conversation: Conversation, senderUserId: UUID, body: String): MessageView {
        val now = Instant.now()
        // RF-053: a block in either direction stops the conversation.
        val recipientId = conversation.counterpartOf(senderUserId)
        if (userBlockRepository.existsBetween(senderUserId, recipientId)) {
            throw ForbiddenException("The conversation is blocked between these users")
        }
        // The aggregate enforces participation and content limits; the service
        // only decides who is notified.
        val message = Message.send(
            id = UUID.randomUUID(),
            conversation = conversation,
            senderUserId = senderUserId,
            body = body,
            now = now,
        )
        messageRepository.save(message)

        notificationRepository.save(
            Notification.forNewMessage(
                id = UUID.randomUUID(),
                userId = recipientId,
                conversationId = conversation.id,
                now = now,
            ),
        )
        return message.toView()
    }
}