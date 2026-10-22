package com.coliving.api.messaging.application.event

import java.util.UUID

/**
 * Raised inside the message transaction and consumed after commit, so no
 * external channel (e-mail/FCM) fires if the message is rolled back.
 */
data class NewMessageEvent(
    val recipientUserId: UUID,
    val conversationId: UUID,
)
