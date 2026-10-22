package com.coliving.api.messaging.application.usecase

import com.coliving.api.messaging.application.dto.ConversationDetailView
import com.coliving.api.messaging.application.dto.ConversationView
import com.coliving.api.messaging.application.dto.ListingContextView
import com.coliving.api.messaging.application.dto.MessageView
import com.coliving.api.messaging.application.dto.OpenConversationCommand
import com.coliving.api.messaging.application.port.out.ListingContact
import com.coliving.api.messaging.application.port.out.ListingContactPort
import com.coliving.api.messaging.application.port.out.ReservationPartiesPort
import com.coliving.api.messaging.domain.model.Conversation
import com.coliving.api.messaging.domain.model.Message
import com.coliving.api.messaging.domain.repository.ConversationRepository
import com.coliving.api.messaging.domain.repository.MessageRepository
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.InvalidArgumentException
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Conversations (RF-050), now also openable directly about a room before a
 * request exists. Opening is idempotent: both parties land on the same thread.
 * Only the two parties may read it, enforced here from local state.
 */
@Service
class ConversationService(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val reservationPartiesPort: ReservationPartiesPort,
    private val listingContactPort: ListingContactPort,
) {

    @Transactional
    fun open(command: OpenConversationCommand): ConversationDetailView {
        if (command.reservationId == null && command.unitId == null) {
            throw InvalidArgumentException("Indica una solicitud o una habitación")
        }
        val conversation = when {
            command.reservationId != null -> openReservation(command.reservationId, command.actorId)
            else -> openListing(command.unitId!!, command.actorId)
        }
        return buildDetail(conversation)
    }

    private fun openReservation(reservationId: UUID, actorId: UUID): Conversation {
        val parties = reservationPartiesPort.findParties(reservationId)
            ?: throw NotFoundException("Solicitud no encontrada")
        if (actorId != parties.guestUserId && actorId != parties.hostUserId) {
            throw ForbiddenException("No eres parte de esta solicitud")
        }
        return conversationRepository.findByReservation(reservationId)
            ?: Conversation.forReservation(
                id = UUID.randomUUID(),
                reservationId = parties.reservationId,
                unitId = parties.unitId,
                publicationId = parties.publicationId,
                subject = null,
                guestUserId = parties.guestUserId,
                hostUserId = parties.hostUserId,
                now = Instant.now(),
            ).also { conversationRepository.save(it) }
    }

    private fun openListing(unitId: UUID, actorId: UUID): Conversation {
        val listing = listingContactPort.findListing(unitId)
            ?: throw NotFoundException("Habitación no encontrada")
        if (actorId == listing.hostId) {
            throw ConflictException("No puedes abrir una conversación contigo mismo")
        }
        return conversationRepository.findByListingAndGuest(unitId, actorId)
            ?: Conversation.forListing(
                id = UUID.randomUUID(),
                unitId = listing.unitId,
                publicationId = listing.publicationId,
                subject = "${listing.propertyTitle} · ${listing.roomTitle}",
                guestUserId = actorId,
                hostUserId = listing.hostId,
                now = Instant.now(),
            ).also { conversationRepository.save(it) }
    }

    @Transactional(readOnly = true)
    fun listMine(userId: UUID): List<ConversationView> =
        conversationRepository.findByParticipant(userId).map { buildSummary(it) }

    @Transactional(readOnly = true)
    fun get(conversationId: UUID, requesterId: UUID): ConversationDetailView {
        val conversation = conversationRepository.findById(conversationId)
            ?: throw NotFoundException("Conversación no encontrada")
        conversation.requireParticipant(requesterId)
        return buildDetail(conversation)
    }

    private fun buildDetail(conversation: Conversation): ConversationDetailView =
        ConversationDetailView(
            conversation = buildSummary(conversation),
            listing = listingContext(conversation),
            messages = messageRepository.findByConversation(conversation.id).map { it.toView() },
        )

    private fun buildSummary(conversation: Conversation): ConversationView {
        val listing = conversation.unitId?.let(listingContactPort::findListing)
        return ConversationView(
            id = conversation.id,
            reservationId = conversation.reservationId,
            unitId = conversation.unitId,
            publicationId = conversation.publicationId,
            subject = conversation.subject,
            guestUserId = conversation.guestUserId,
            hostUserId = conversation.hostUserId,
            createdAt = conversation.createdAt,
            messageCount = messageRepository.countByConversation(conversation.id),
            lastMessageAt = messageRepository.findLastByConversation(conversation.id)?.sentAt,
            roomTitle = listing?.roomTitle,
            propertyTitle = listing?.propertyTitle,
            cityName = listing?.cityName,
        )
    }

    private fun listingContext(conversation: Conversation): ListingContextView {
        val listing: ListingContact? = conversation.unitId?.let(listingContactPort::findListing)
        return ListingContextView(
            unitId = conversation.unitId,
            publicationId = conversation.publicationId,
            roomTitle = listing?.roomTitle,
            propertyTitle = listing?.propertyTitle,
            cityName = listing?.cityName,
            reservationId = conversation.reservationId,
        )
    }
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
