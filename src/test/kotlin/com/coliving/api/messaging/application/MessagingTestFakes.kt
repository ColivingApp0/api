package com.coliving.api.messaging.application

import com.coliving.api.messaging.application.port.out.ReportCasePort
import com.coliving.api.messaging.application.port.out.PushSender
import com.coliving.api.messaging.application.port.out.RecipientContact
import com.coliving.api.messaging.application.port.out.RecipientContactPort
import com.coliving.api.messaging.application.port.out.ListingContact
import com.coliving.api.messaging.application.port.out.ListingContactPort
import com.coliving.api.messaging.application.port.out.ReservationParties
import com.coliving.api.messaging.application.port.out.ReservationPartiesPort
import com.coliving.api.messaging.domain.model.Conversation
import com.coliving.api.messaging.domain.model.ConversationReport
import com.coliving.api.messaging.domain.model.Message
import com.coliving.api.messaging.domain.model.Notification
import com.coliving.api.messaging.domain.model.PushToken
import com.coliving.api.messaging.domain.model.UserBlock
import com.coliving.api.messaging.domain.repository.ConversationReportRepository
import com.coliving.api.messaging.domain.repository.ConversationRepository
import com.coliving.api.messaging.domain.repository.MessageRepository
import com.coliving.api.messaging.domain.repository.NotificationRepository
import com.coliving.api.messaging.domain.repository.PushTokenRepository
import com.coliving.api.messaging.domain.repository.UserBlockRepository
import java.util.UUID
import org.springframework.context.ApplicationEventPublisher

/** In-memory stand-in of booking's ReservationMessagingQuery (RF-050). */
class FakeReservationPartiesPort : ReservationPartiesPort {
    val parties = mutableMapOf<UUID, ReservationParties>()

    override fun findParties(reservationId: UUID): ReservationParties? = parties[reservationId]

    fun add(
        reservationId: UUID = UUID.randomUUID(),
        guestUserId: UUID,
        hostUserId: UUID,
        unitId: UUID = UUID.randomUUID(),
        publicationId: UUID = UUID.randomUUID(),
    ): ReservationParties {
        val value = ReservationParties(
            reservationId = reservationId,
            unitId = unitId,
            publicationId = publicationId,
            guestUserId = guestUserId,
            hostUserId = hostUserId,
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

    override fun findByListingAndGuest(unitId: UUID, guestUserId: UUID): Conversation? =
        store.values.firstOrNull {
            it.unitId == unitId && it.guestUserId == guestUserId && it.reservationId == null
        }

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

class FakeUserBlockRepository : UserBlockRepository {    val store = mutableMapOf<UUID, UserBlock>()

    override fun existsBetween(userA: UUID, userB: UUID): Boolean =
        store.values.any {
            (it.blockerUserId == userA && it.blockedUserId == userB) ||
                (it.blockerUserId == userB && it.blockedUserId == userA)
        }

    override fun findByBlocker(blockerUserId: UUID): List<UserBlock> =
        store.values.filter { it.blockerUserId == blockerUserId }
            .sortedByDescending { it.createdAt }

    override fun findByBlockerAndBlocked(blockerUserId: UUID, blockedUserId: UUID): UserBlock? =
        store.values.firstOrNull { it.blockerUserId == blockerUserId && it.blockedUserId == blockedUserId }

    override fun save(block: UserBlock) {
        store[block.blockerUserId] = block
    }

    override fun delete(block: UserBlock) {
        store.remove(block.blockerUserId)
    }
}

class FakeConversationReportRepository : ConversationReportRepository {
    val store = mutableListOf<ConversationReport>()

    override fun findByReporter(reporterUserId: UUID): List<ConversationReport> =
        store.filter { it.reporterUserId == reporterUserId }.sortedByDescending { it.createdAt }

    override fun findByConversationAndReporter(
        conversationId: UUID,
        reporterUserId: UUID,
    ): ConversationReport? = store.firstOrNull {
        it.conversationId == conversationId && it.reporterUserId == reporterUserId
    }

    override fun save(report: ConversationReport) {
        store.add(report)
    }
}

class FakeReportCasePort : ReportCasePort {
    val openedCases = mutableListOf<UUID>()

    override fun openConversationReportCase(
        conversationId: UUID,
        reportedUserId: UUID,
        reporterUserId: UUID,
        reason: String,
    ): UUID = UUID.randomUUID().also { openedCases.add(it) }
}

class FakePushTokenRepository : PushTokenRepository {
    val store = mutableMapOf<String, PushToken>()

    override fun findByUser(userId: UUID): List<PushToken> =
        store.values.filter { it.userId == userId }

    override fun findByToken(token: String): PushToken? = store[token]

    override fun save(token: PushToken) {
        store[token.token] = token
    }

    override fun delete(token: PushToken) {
        store.remove(token.token)
    }
}

class FakePushSender : PushSender {
    val sent = mutableListOf<Pair<UUID, UUID>>()

    override fun sendNewMessage(userId: UUID, conversationId: UUID) {
        sent.add(userId to conversationId)
    }
}

class FakeApplicationEventPublisher : ApplicationEventPublisher {
    val published = mutableListOf<Any>()

    override fun publishEvent(event: Any) {
        published.add(event)
    }
}

class FakeRecipientContactPort : RecipientContactPort {
    val contacts = mutableMapOf<UUID, RecipientContact>()

    override fun contactOf(userId: UUID): RecipientContact? = contacts[userId]
}


/** In-memory stand-in of accommodation's listing contact query. */
class FakeListingContactPort : ListingContactPort {
    val listings = mutableMapOf<UUID, ListingContact>()

    override fun findListing(unitId: UUID): ListingContact? = listings[unitId]

    fun add(
        unitId: UUID = UUID.randomUUID(),
        publicationId: UUID = UUID.randomUUID(),
        hostId: UUID,
        roomTitle: String = "Habitación privada",
        propertyTitle: String = "Casa compartida",
        cityName: String = "Bogotá",
    ): ListingContact {
        val value = ListingContact(unitId, publicationId, hostId, roomTitle, propertyTitle, cityName)
        listings[unitId] = value
        return value
    }
}
