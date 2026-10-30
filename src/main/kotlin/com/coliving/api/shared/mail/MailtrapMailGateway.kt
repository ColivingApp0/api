package com.coliving.api.shared.mail

import io.mailtrap.client.MailtrapClient
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * Mailtrap Email Sending transport. It adapts the shared [MailGateway] port to
 * the official `io.mailtrap:mailtrap-java` SDK; no application service knows
 * about Mailtrap.
 *
 * The token and sandbox settings come from `app.mail.mailtrap.*`
 * (`MAILTRAP_TOKEN`, `MAILTRAP_SANDBOX`, `MAILTRAP_INBOX_ID`).
 */
@Component
@ConditionalOnProperty(name = ["app.mail.provider"], havingValue = "mailtrap")
class MailtrapMailGateway(
    private val client: MailtrapClient,
    private val properties: MailProperties,
) : MailGateway {

    private val logger = LoggerFactory.getLogger(MailtrapMailGateway::class.java)

    override fun send(message: MailMessage) {
        val response = client.send(MailtrapMailMapper.toMail(message, properties))
        if (!response.isSuccess) {
            throw IllegalStateException(
                "Mailtrap rejected the message (ids=${response.messageIds})",
            )
        }
        logger.info(
            "[mail:mailtrap] sent category={} ids={}",
            message.category,
            response.messageIds,
        )
    }
}
