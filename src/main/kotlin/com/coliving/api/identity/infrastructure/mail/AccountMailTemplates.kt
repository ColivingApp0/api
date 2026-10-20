package com.coliving.api.identity.infrastructure.mail

import com.coliving.api.shared.mail.MailMessage
import com.coliving.api.shared.mail.MailProperties
import org.springframework.web.util.UriComponentsBuilder

/**
 * Content of the two account e-mails (RF-001 / RF-003). Links point at the
 * Angular routes that already accept `?token=`, so the same template works for
 * every transport.
 */
object AccountMailTemplates {

    fun verification(properties: MailProperties, to: String, token: String): MailMessage {
        val link = link(properties, properties.verificationPath, token)
        return MailMessage(
            to = to,
            subject = "Verifica tu correo en ColivinApp",
            text = """
                Hola:
                Confirma tu correo para activar tu cuenta en ColivinApp.
                Abre este enlace: $link
                Si no creaste la cuenta, ignora este mensaje.
            """.trimIndent(),
            html = """
                <p>Hola:</p>
                <p>Confirma tu correo para activar tu cuenta en <strong>ColivinApp</strong>.</p>
                <p><a href="$link">Verificar mi correo</a></p>
                <p>Si no creaste la cuenta, ignora este mensaje.</p>
            """.trimIndent(),
            category = "account",
        )
    }

    fun passwordReset(properties: MailProperties, to: String, token: String): MailMessage {
        val link = link(properties, properties.resetPath, token)
        return MailMessage(
            to = to,
            subject = "Restablece tu contraseña de ColivinApp",
            text = """
                Hola:
                Recibimos una solicitud para restablecer tu contraseña.
                Abre este enlace: $link
                Si no la solicitaste, ignora este mensaje; tu contraseña no cambia.
            """.trimIndent(),
            html = """
                <p>Hola:</p>
                <p>Recibimos una solicitud para restablecer tu contraseña.</p>
                <p><a href="$link">Restablecer mi contraseña</a></p>
                <p>Si no la solicitaste, ignora este mensaje; tu contraseña no cambia.</p>
            """.trimIndent(),
            category = "account",
        )
    }

    private fun link(properties: MailProperties, path: String, token: String): String =
        UriComponentsBuilder.fromUriString(properties.publicBaseUrl)
            .path(path)
            .queryParam("token", token)
            .build()
            .encode()
            .toUriString()
}
