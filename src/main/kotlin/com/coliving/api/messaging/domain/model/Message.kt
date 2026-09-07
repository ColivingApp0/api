package com.coliving.api.messaging.domain.model

import com.coliving.api.shared.error.InvalidArgumentException
import java.time.Instant
import java.util.UUID

/**
 * A single message inside a [Conversation] (RF-050): always ordered by
 * [sentAt] and always identified by its [senderUserId]. Only the parties of
 * the conversation may write, which is enforced by [Conversation].
 *
 * Basic content limits (RF-053 groundwork) keep an empty or abusive payload
 * out of storage.
 */
class Message(
    val id: UUID,
    val conversationId: UUID,
    val senderUserId: UUID,
    val body: String,
    val sentAt: Instant,
) {

    companion object {
        const val MAX_BODY_LENGTH = 2000

        fun send(
            id: UUID,
            conversation: Conversation,
            senderUserId: UUID,
            body: String,
            now: Instant,
        ): Message {
            conversation.requireParticipant(senderUserId)
            val normalized = body.trim()
            if (normalized.isEmpty()) {
                throw InvalidArgumentException("Message body must not be empty")
            }
            if (normalized.length > MAX_BODY_LENGTH) {
                throw InvalidArgumentException("Message body must not exceed $MAX_BODY_LENGTH characters")
            }
            return Message(
                id = id,
                conversationId = conversation.id,
                senderUserId = senderUserId,
                body = normalized,
                sentAt = now,
            )
        }
    }
}
