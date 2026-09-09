package com.coliving.api.messaging.application

import com.coliving.api.messaging.application.port.out.ReservationParties
import com.coliving.api.messaging.application.port.out.ReservationPartiesPort
import com.coliving.api.messaging.domain.model.Conversation
import com.coliving.api.messaging.domain.model.Message
import com.coliving.api.messaging.domain.model.Notification
import com.coliving.api.messaging.domain.repository.ConversationRepository
import com.coliving.api.messaging.domain.repository.MessageRepository
import com.coliving.api.messaging.domain.repository.NotificationRepository
import java.time.LocalDate
import java.util.UUID

/** In-memory stand-in of booking's ReservationMessagingQuery (RF-050). */
class FakeReservationPartiesPort : ReservationPartiesPort {
    val parties = mutableMapOf<UUID, ReservationParties>()

    override fun findParties(reservationId: UUID): ReservationParties? = parties[reservationId]

    fun add(
        reservationId: UUID = UUID.randomUUID(),
        guestUserId: UUID,
        hostUserId: UUID,
        fromDate: LocalDate = LocalDate.now().plusDays(10),
        toDate: LocalDate = LocalDate.now().plusDays(14),
    ): ReservationParties {
        val value = ReservationParties(
            reservationId = reservationId,
            guestUserId = guestUserId,
            hostUserId = hostUserId,
            fromDate = fromDate,
            toDate = toDate,
        )
        parties[reservationId] = value
        return value
    }
}

class FakeConversationRepository : ConversationRepository {
    val store = mutableMapOf<UUID, Conversation>()

    override fun findById(id: UUID): Conversation? = store[id]

    override fun findByReservation(reservationId: UUID): Conversation? =
        store.values.firstOrNull { it.reservationId == reservationId }

    override fun findByParticipant(userId: UUID): List<Conversation> =
        store.values
            .filter { it.isParticipant(userId) }
            .sortedByDescending { it.createdAt }

    override fun save(conversation: Conversation) {
        store[conversation.id] = conversation
    }
}

class FakeMessageRepository : MessageRepository {
    val store = mutableListOf<Message>()

    override fun findByConversation(conversationId: UUID): List<Message> =
        store.filter { it.conversationId == conversationId }.sortedBy { it.sentAt }

    override fun countByConversation(conversationId: UUID): Int =
        store.count { it.conversationId == conversationId }

    override fun findLastByConversation(conversationId: UUID): Message? =
        findByConversation(conversationId).lastOrNull()

    override fun save(message: Message) {
        store.add(message)
    }
}

class FakeNotificationRepository : NotificationRepository {
    val store = mutableMapOf<UUID, Notification>()

    override fun findById(id: UUID): Notification? = store[id]

    override fun findByUser(userId: UUID): List<Notification> =
        store.values.filter { it.userId == userId }.sortedByDescending { it.createdAt }

    override fun findUnreadByUser(userId: UUID): List<Notification> =
        findByUser(userId).filter { it.isUnread() }

    override fun save(notification: Notification) {
        store[notification.id] = notification
    }
}
