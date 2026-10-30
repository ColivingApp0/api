package com.coliving.api.messaging.infrastructure.push

import com.coliving.api.messaging.application.port.out.PushSender
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * Default push transport: logs the event and keeps the application working
 * without Firebase credentials. It is the local/test behaviour; production
 * enables FCM with `app.push.enabled=true`.
 */
@Component
@ConditionalOnProperty(name = ["app.push.enabled"], havingValue = "false", matchIfMissing = true)
class LoggingPushSender : PushSender {

    private val logger = LoggerFactory.getLogger(LoggingPushSender::class.java)

    override fun sendNewMessage(userId: UUID, conversationId: UUID) {
        logger.info(
            "[push:log] user={} conversation={} (FCM disabled)",
            userId,
            conversationId,
        )
    }
}
