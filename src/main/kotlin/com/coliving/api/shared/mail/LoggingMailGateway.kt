package com.coliving.api.shared.mail

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * Pilot transport: logs the message instead of delivering it. It is the default
 * so local development keeps working without SMTP credentials and the
 * verification/reset tokens stay visible in the API log (as before).
 */
@Component
@ConditionalOnProperty(name = ["app.mail.provider"], havingValue = "log", matchIfMissing = true)
class LoggingMailGateway : MailGateway {

    private val logger = LoggerFactory.getLogger(LoggingMailGateway::class.java)

    override fun send(message: MailMessage) {
        logger.info(
            "[mail:log] to={} subject={} text={}",
            message.to,
            message.subject,
            message.text,
        )
    }
}
