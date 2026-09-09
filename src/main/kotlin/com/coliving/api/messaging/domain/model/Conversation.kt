package com.coliving.api.messaging.domain.model

import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import java.time.Instant
import java.util.UUID

/**
 * Conversation between the two parties of a reservation (RF-050): the guest
 * who requested the stay and the host who owns the unit. There is at most one
 * conversation per reservation — reopening it returns the existing thread
 * instead of creating a second one.
 *
 * The parties are copied from `booking` (through ReservationPartiesPort) at
 * creation time, so authorization inside this context needs no cross-context
 * call: [isParticipant] answers from local state.
 */
class Conversation(
    val id: UUID,
    val reservationId: UUID,
    val guestUserId: UUID,
    val hostUserId: UUID,
    val createdAt: Instant,
) {

    /** Whether [userId] is allowed to read or write in this conversation. */
    fun isParticipant(userId: UUID): Boolean =
        userId == guestUserId || userId == hostUserId

    /**
     * The other party — the recipient of a notification for a message sent by
     * [senderId] (RF-051).
     */
    fun counterpartOf(senderId: UUID): UUID = when (senderId) {
        guestUserId -> hostUserId
        hostUserId -> guestUserId
        else -> throw ForbiddenException("Not a party of this conversation")
    }

    /** Fails unless [userId] is one of the two parties. */
    fun requireParticipant(userId: UUID) {
        if (!isParticipant(userId)) {
            throw ForbiddenException("Not a party of this conversation")
        }
    }

    companion object {
        fun start(
            id: UUID,
            reservationId: UUID,
            guestUserId: UUID,
            hostUserId: UUID,
            now: Instant,
        ): Conversation {
            if (guestUserId == hostUserId) {
                throw ConflictException(
                    "A conversation needs two distinct parties",
                )
            }
            return Conversation(
                id = id,
                reservationId = reservationId,
                guestUserId = guestUserId,
                hostUserId = hostUserId,
                createdAt = now,
            )
        }
    }
}
