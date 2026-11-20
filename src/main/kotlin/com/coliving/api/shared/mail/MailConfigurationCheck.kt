package com.coliving.api.shared.mail

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * Startup diagnostics for the outbound e-mail configuration.
 *
 * Misconfiguration used to fail silently: `provider=log` in production accepts
 * the request (HTTP 202) but never delivers the message, and a `publicBaseUrl`
 * pointing at `localhost` builds links that only work on the developer machine.
 * This listener logs a very visible WARN in those cases but never blocks the
 * application startup. Tokens are never printed here.
 */
@Component
class MailConfigurationCheck(
    private val properties: MailProperties,
    private val environment: Environment,
) {

    private val logger = LoggerFactory.getLogger(MailConfigurationCheck::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun check() {
        val dev = isDevelopmentProfile()
        if (properties.provider == MailProperties.Provider.LOG && !dev) {
            logger.warn(
                "MAIL CONFIGURATION: app.mail.provider=log outside the dev profile. " +
                    "E-mails are only written to the log and are NOT delivered. " +
                    "Set MAIL_PROVIDER=mailtrap, smtp or firebase for real delivery.",
            )
        }
        if (!dev && properties.publicBaseUrl.contains("localhost", ignoreCase = true)) {
            logger.warn(
                "MAIL CONFIGURATION: app.mail.publicBaseUrl='{}' points at localhost outside the dev profile. " +
                    "Verification and password-reset links will not work for users. " +
                    "Set APP_PUBLIC_BASE_URL to the public frontend origin.",
                properties.publicBaseUrl,
            )
        }
    }

    private fun isDevelopmentProfile(): Boolean =
        environment.activeProfiles.any { it.equals("dev", ignoreCase = true) }
}
