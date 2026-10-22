package com.coliving.api.messaging.application.usecase

import com.coliving.api.messaging.application.event.NewMessageEvent
import com.coliving.api.messaging.application.port.out.PushSender
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Fan-out of the in-app notification to the external channels (RF-051).
 *
 * Runs **after commit**, so a rolled-back message never produces an e-mail or a
 * push. Each channel is isolated: a provider outage is logged and does not
 * prevent the other channel (or the already-committed message) from working.
 */
@Component
class NewMessageNotificationListener(
    private val pushSender: PushSender,
    private val emailNotifier: MessageEmailNotifier,
) {

    private val logger = LoggerFactory.getLogger(NewMessageNotificationListener::class.java)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onNewMessage(event: NewMessageEvent) {
        runCatching {
            pushSender.sendNewMessage(event.recipientUserId, event.conversationId)
        }.onFailure {
            logger.warn(
                "Push notification failed for user {} and conversation {}",
                event.recipientUserId,
                event.conversationId,
                it,
            )
        }

        runCatching {
            emailNotifier.sendNewMessage(event.recipientUserId, event.conversationId)
        }.onFailure {
            logger.warn(
                "E-mail notification failed for user {} and conversation {}",
                event.recipientUserId,
                event.conversationId,
                it,
            )
        }
    }
}
