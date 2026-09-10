package com.coliving.api.messaging.application.usecase

import com.coliving.api.messaging.application.dto.ConversationDetailView
import com.coliving.api.messaging.application.dto.ConversationView
import com.coliving.api.messaging.application.dto.MessageView
import com.coliving.api.messaging.application.dto.OpenConversationCommand
import com.coliving.api.messaging.application.dto.ReservationStayView
import com.coliving.api.messaging.application.port.out.ReservationParties
import com.coliving.api.messaging.application.port.out.ReservationPartiesPort
import com.coliving.api.messaging.domain.model.Conversation
import com.coliving.api.messaging.domain.model.Message
import com.coliving.api.messaging.domain.repository.ConversationRepository
import com.coliving.api.messaging.domain.repository.MessageRepository
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Conversation of a reservation (RF-050). Opening it is idempotent: the guest
 * and the host land on the same thread, which is created lazily the first time
 * one of the parties asks for it. Only the two parties may read it, and their
 * identity is resolved from `booking` (ReservationPartiesPort) — this context
 * never reads reservation tables.
 */
@Service
class ConversationService(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val reservationPartiesPort: ReservationPartiesPort,
) {

    @Transactional
    fun open(command: OpenConversationCommand): ConversationDetailView {
        val parties = reservationPartiesPort.findParties(command.reservationId)
            ?: throw NotFoundException("Reservation not found")
        if (command.actorId != parties.guestUserId && command.actorId != parties.hostUserId) {
            throw ForbiddenException("Not a party of this reservation")
        }

        // Idempotent: both parties always land on the same thread (RF-050).
        val conversation = conversationRepository.findByReservation(command.reservationId)
            ?: Conversation.start(
                id = UUID.randomUUID(),
                reservationId = parties.reservationId,
                guestUserId = parties.guestUserId,
                hostUserId = parties.hostUserId,
                now = Instant.now(),
            ).also { conversationRepository.save(it) }

        return buildDetail(conversation, parties)
    }

    /**
     * Threads of a user, whichever side they are on. The summary carries no stay
     * dates on purpose: listing threads must not reach into `booking`.
     */
    @Transactional(readOnly = true)
    fun listMine(userId: UUID): List<ConversationView> =
        conversationRepository.findByParticipant(userId).map { buildSummary(it) }

    @Transactional(readOnly = true)
    fun get(conversationId: UUID, requesterId: UUID): ConversationDetailView {
        val conversation = conversationRepository.findById(conversationId)
            ?: throw NotFoundException("Conversation not found")
        conversation.requireParticipant(requesterId)
        val parties = reservationPartiesPort.findParties(conversation.reservationId)
            ?: throw NotFoundException("Reservation not found")
        return buildDetail(conversation, parties)
    }

    private fun buildDetail(
        conversation: Conversation,
        parties: ReservationParties,
    ): ConversationDetailView =
        ConversationDetailView(
            conversation = buildSummary(conversation),
            stay = ReservationStayView(
                reservationId = parties.reservationId,
                fromDate = parties.fromDate,
                toDate = parties.toDate,
            ),
            messages = messageRepository.findByConversation(conversation.id).map { it.toView() },
        )

    private fun buildSummary(conversation: Conversation): ConversationView =
        ConversationView(
            id = conversation.id,
            reservationId = conversation.reservationId,
            guestUserId = conversation.guestUserId,
            hostUserId = conversation.hostUserId,
            createdAt = conversation.createdAt,
            messageCount = messageRepository.countByConversation(conversation.id),
            lastMessageAt = messageRepository.findLastByConversation(conversation.id)?.sentAt,
        )
}


/** Shared message projection. */
internal fun Message.toView(): MessageView =
    MessageView(
        id = id,
        conversationId = conversationId,
        senderUserId = senderUserId,
        body = body,
        sentAt = sentAt,
    )
