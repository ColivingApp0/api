package com.coliving.api.messaging.application.usecase

import com.coliving.api.messaging.application.port.out.RecipientContactPort
import com.coliving.api.shared.mail.MailGateway
import com.coliving.api.shared.mail.MailMessage
import com.coliving.api.shared.mail.MailProperties
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder

/**
 * E-mail side of the external notification channel (RF-051). It deliberately
 * mirrors the in-app notification: a generic subject and a link to the thread,
 * never the message body or reservation details.
 */
@Service
class MessageEmailNotifier(
    private val recipientContactPort: RecipientContactPort,
    private val mailGateway: MailGateway,
    private val properties: MailProperties,
) {

    private val logger = LoggerFactory.getLogger(MessageEmailNotifier::class.java)

    fun sendNewMessage(userId: UUID, conversationId: UUID) {
        if (!properties.messageNotificationsEnabled) {
            return
        }
        val contact = recipientContactPort.contactOf(userId)
        if (contact == null) {
            logger.warn("Cannot e-mail new message: user {} has no contact", userId)
            return
        }
        val link = UriComponentsBuilder.fromUriString(properties.publicBaseUrl)
            .path("/mensajes/{id}")
            .buildAndExpand(conversationId)
            .toUriString()
        mailGateway.send(
            MailMessage(
                to = contact.email,
                subject = "Tienes un nuevo mensaje en ColivinApp",
                text = """
                    Hola:
                    Tienes un nuevo mensaje en una conversación de ColivinApp.
                    Abre este enlace para leerlo: $link
                """.trimIndent(),
                html = """
                    <p>Hola:</p>
                    <p>Tienes un nuevo mensaje en una conversación de <strong>ColivinApp</strong>.</p>
                    <p><a href="$link">Leer la conversación</a></p>
                """.trimIndent(),
                category = "messages",
            ),
        )
    }
}
