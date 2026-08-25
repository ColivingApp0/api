package com.coliving.api.identity.infrastructure.mail

import com.coliving.api.identity.application.port.out.EmailSender
import com.coliving.api.identity.domain.vo.Email
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Pilot stub for the [EmailSender] port: logs what would be mailed.
 * In production this must be replaced by a real provider (SMTP/REST); the
 * port boundary keeps the application layer unchanged.
 */
@Component
class LoggingEmailSender : EmailSender {

    private val logger = LoggerFactory.getLogger(LoggingEmailSender::class.java)

    override fun sendVerificationEmail(to: Email, rawToken: String) {
        logger.info(
            "[dev-email] verify account email={} token={}",
            to.value,
            rawToken,
        )
    }

    override fun sendPasswordResetEmail(to: Email, rawToken: String) {
        logger.info(
            "[dev-email] reset password email={} token={}",
            to.value,
            rawToken,
        )
    }
}