package com.coliving.api.identity.application.port.out

import com.coliving.api.identity.domain.vo.Email

/**
 * Out port for outbound e-mail in the identity flows (RF-001, RF-003).
 * The physical transport (SMTP provider) is replaceable behind this port;
 * the pilot ships a logging stub in infrastructure.
 */
interface EmailSender {
    fun sendVerificationEmail(to: Email, rawToken: String)
    fun sendPasswordResetEmail(to: Email, rawToken: String)
}