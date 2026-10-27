package com.coliving.api.shared.mail

/**
 * Outbound e-mail port. Implementations are selected by
 * `app.mail.provider` (`log`, `smtp` or `firebase`).
 */
interface MailGateway {
    fun send(message: MailMessage)
}
