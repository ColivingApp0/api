package com.coliving.api.identity.infrastructure.mail

import com.coliving.api.identity.domain.vo.Email
import com.coliving.api.shared.mail.MailGateway
import com.coliving.api.shared.mail.MailMessage
import com.coliving.api.shared.mail.MailProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountEmailSenderTest {

    private val gateway = CapturingMailGateway()
    private val properties = MailProperties(
        publicBaseUrl = "https://app.coliving.test",
        verificationPath = "/verificar-correo",
        resetPath = "/restablecer-password",
    )
    private val sender = AccountEmailSender(gateway, properties)

    @Test
    fun `verification e-mail points at the Angular route with the token`() {
        sender.sendVerificationEmail(Email.of("ada@example.com"), "verify-token")

        val message = gateway.sent.single()
        assertEquals("ada@example.com", message.to)
        assertTrue(message.subject.contains("Verifica"))
        assertTrue(
            message.text.contains(
                "https://app.coliving.test/verificar-correo?token=verify-token",
            ),
        )
        assertTrue(message.html!!.contains("https://app.coliving.test/verificar-correo?token=verify-token"))
    }

    @Test
    fun `password reset e-mail points at the reset route`() {
        sender.sendPasswordResetEmail(Email.of("ada@example.com"), "reset-token")

        val message = gateway.sent.single()
        assertEquals("ada@example.com", message.to)
        assertTrue(message.subject.contains("Restablece"))
        assertTrue(
            message.text.contains(
                "https://app.coliving.test/restablecer-password?token=reset-token",
            ),
        )
    }
}

class CapturingMailGateway : MailGateway {
    val sent = mutableListOf<MailMessage>()

    override fun send(message: MailMessage) {
        sent.add(message)
    }
}
