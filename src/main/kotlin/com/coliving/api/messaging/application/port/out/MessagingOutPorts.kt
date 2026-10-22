package com.coliving.api.messaging.application.port.out

import java.util.UUID

/** Parties of a reservation as read from `booking`. */
data class ReservationParties(
    val reservationId: UUID,
    val unitId: UUID,
    val publicationId: UUID,
    val guestUserId: UUID,
    val hostUserId: UUID,
)

interface ReservationPartiesPort {
    fun findParties(reservationId: UUID): ReservationParties?
}

/** Room context as read from `accommodation` for listing conversations. */
data class ListingContact(
    val unitId: UUID,
    val publicationId: UUID,
    val hostId: UUID,
    val roomTitle: String,
    val propertyTitle: String,
    val cityName: String?,
)

interface ListingContactPort {
    fun findListing(unitId: UUID): ListingContact?
}

interface ReportCasePort {

    fun openConversationReportCase(
        conversationId: UUID,
        reportedUserId: UUID,
        reporterUserId: UUID,
        reason: String,
    ): UUID
}

data class RecipientContact(
    val userId: UUID,
    val email: String,
)

interface RecipientContactPort {
    fun contactOf(userId: UUID): RecipientContact?
}

interface PushSender {
    fun sendNewMessage(userId: UUID, conversationId: UUID)
}
