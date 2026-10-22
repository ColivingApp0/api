package com.coliving.api.messaging.domain.model

import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import java.time.Instant
import java.util.UUID

/**
 * Conversation between a guest and a host (RF-050). It can be attached to a
 * reservation or opened directly about a room listing (before requesting a
 * place). The parties are copied at creation time, so authorization needs no
 * cross-context call.
 */
class Conversation(
    val id: UUID,
    val reservationId: UUID?,
    val unitId: UUID?,
    val publicationId: UUID?,
    val subject: String?,
    val guestUserId: UUID,
    val hostUserId: UUID,
    val createdAt: Instant,
) {

    fun isParticipant(userId: UUID): Boolean =
        userId == guestUserId || userId == hostUserId

    fun counterpartOf(senderId: UUID): UUID = when (senderId) {
        guestUserId -> hostUserId
        hostUserId -> guestUserId
        else -> throw ForbiddenException("No eres parte de esta conversación")
    }

    fun requireParticipant(userId: UUID) {
        if (!isParticipant(userId)) {
            throw ForbiddenException("No eres parte de esta conversación")
        }
    }

    companion object {
        fun forReservation(
            id: UUID,
            reservationId: UUID,
            unitId: UUID?,
            publicationId: UUID?,
            subject: String?,
            guestUserId: UUID,
            hostUserId: UUID,
            now: Instant,
        ): Conversation = build(
            id, reservationId, unitId, publicationId, subject, guestUserId, hostUserId, now,
        )

        fun forListing(
            id: UUID,
            unitId: UUID,
            publicationId: UUID,
            subject: String,
            guestUserId: UUID,
            hostUserId: UUID,
            now: Instant,
        ): Conversation = build(
            id, null, unitId, publicationId, subject, guestUserId, hostUserId, now,
        )

        private fun build(
            id: UUID,
            reservationId: UUID?,
            unitId: UUID?,
            publicationId: UUID?,
            subject: String?,
            guestUserId: UUID,
            hostUserId: UUID,
            now: Instant,
        ): Conversation {
            if (guestUserId == hostUserId) {
                throw ConflictException("Una conversación necesita dos participantes distintos")
            }
            return Conversation(
                id = id,
                reservationId = reservationId,
                unitId = unitId,
                publicationId = publicationId,
                subject = subject?.trim()?.takeIf { it.isNotBlank() },
                guestUserId = guestUserId,
                hostUserId = hostUserId,
                createdAt = now,
            )
        }
    }
}
