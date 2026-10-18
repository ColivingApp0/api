package com.coliving.api.shared.mail

import io.mailtrap.client.MailtrapClient
import io.mailtrap.config.MailtrapConfig
import io.mailtrap.factory.MailtrapClientFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Builds the Mailtrap SDK client only when `app.mail.provider=mailtrap`.
 *
 * The token is read from `app.mail.mailtrap.token` (normally
 * `MAILTRAP_TOKEN`). By default the Email Sending API is used; set
 * `MAILTRAP_SANDBOX=true` (plus `MAILTRAP_INBOX_ID`) to target the Email
 * Testing sandbox instead.
 */
@Configuration
@ConditionalOnProperty(name = ["app.mail.provider"], havingValue = "mailtrap")
class MailtrapMailConfig {

    @Bean
    fun mailtrapClient(properties: MailProperties): MailtrapClient {
        val mailtrap = properties.mailtrap
        require(mailtrap.token.isNotBlank()) {
            "MAILTRAP_TOKEN is required when app.mail.provider=mailtrap"
        }
        val builder = MailtrapConfig.Builder().token(mailtrap.token)
        if (mailtrap.sandbox) {
            val inboxId = mailtrap.inboxId
                ?: throw IllegalStateException(
                    "MAILTRAP_INBOX_ID is required when MAILTRAP_SANDBOX=true",
                )
            builder.sandbox(true).inboxId(inboxId)
        }
        return MailtrapClientFactory.createMailtrapClient(builder.build())
    }
}
