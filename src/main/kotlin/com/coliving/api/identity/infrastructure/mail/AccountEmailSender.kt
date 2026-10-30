package com.coliving.api.identity.infrastructure.mail

import com.coliving.api.identity.application.port.out.EmailSender
import com.coliving.api.identity.domain.vo.Email
import com.coliving.api.shared.mail.MailGateway
import com.coliving.api.shared.mail.MailProperties
import org.springframework.stereotype.Component

/**
 * Real implementation of the identity [EmailSender] port. It only builds the
 * account e-mails; delivery is delegated to the configured [MailGateway]
 * (log, SMTP or Firebase Trigger Email).
 */
@Component
class AccountEmailSender(
    private val mailGateway: MailGateway,
    private val properties: MailProperties,
) : EmailSender {

    override fun sendVerificationEmail(to: Email, rawToken: String) {
        mailGateway.send(AccountMailTemplates.verification(properties, to.value, rawToken))
    }

    override fun sendPasswordResetEmail(to: Email, rawToken: String) {
        mailGateway.send(AccountMailTemplates.passwordReset(properties, to.value, rawToken))
    }
}
