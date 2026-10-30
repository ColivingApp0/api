package com.coliving.api.shared.mail

/**
 * A provider-agnostic outbound e-mail. The transport (SMTP, Mailtrap, Firebase
 * Trigger Email or a logging stub) is selected with `app.mail.provider`, so
 * application services never depend on a concrete provider.
 *
 * [category] is forwarded to providers that support analytics (Mailtrap);
 * it never contains personal data.
 */
data class MailMessage(
    val to: String,
    val subject: String,
    val text: String,
    val html: String? = null,
    val category: String = "ColivinApp",
)
