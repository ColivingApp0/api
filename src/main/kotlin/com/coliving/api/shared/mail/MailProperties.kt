package com.coliving.api.shared.mail

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Mail configuration bound from `app.mail.*`.
 *
 * - `provider=log` keeps the pilot behaviour (tokens in the application log).
 * - `provider=mailtrap` sends through the official Mailtrap SDK (Email Sending
 *   API by default, Email Testing sandbox when `mailtrap.sandbox=true`).
 * - `provider=smtp` sends through Spring Mail (Google Workspace / Gmail SMTP by
 *   default; any SMTP relay works by overriding `spring.mail.*`).
 * - `provider=firebase` writes to the Firestore collection consumed by the
 *   official Firebase "Trigger Email from Firestore" extension.
 */
@ConfigurationProperties(prefix = "app.mail")
data class MailProperties(
    val provider: Provider = Provider.LOG,
    val from: String = "no-reply@coliving.local",
    val fromName: String = "ColivinApp",
    /** Public frontend origin used to build the links inside the e-mails. */
    val publicBaseUrl: String = "http://localhost:4200",
    val verificationPath: String = "/verificar-correo",
    val resetPath: String = "/restablecer-password",
    /** Firestore collection read by the Firebase Trigger Email extension. */
    val firebaseCollection: String = "mail",
    /** Whether a new message also triggers an e-mail to the recipient. */
    val messageNotificationsEnabled: Boolean = true,
    /** Mailtrap Email Sending API (or Email Testing sandbox). */
    val mailtrap: Mailtrap = Mailtrap(),
) {
    enum class Provider {
        LOG,
        SMTP,
        MAILTRAP,
        FIREBASE,
    }

    data class Mailtrap(
        /** API token from Mailtrap → Sending Domains / API tokens. */
        val token: String = "",
        /** `true` uses the Email Testing sandbox instead of the Sending API. */
        val sandbox: Boolean = false,
        /** Sandbox inbox id (required when [sandbox] is true). */
        val inboxId: Long? = null,
    )
}
