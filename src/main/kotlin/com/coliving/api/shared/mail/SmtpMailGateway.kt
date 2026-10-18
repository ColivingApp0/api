package com.coliving.api.shared.mail

import jakarta.mail.internet.MimeMessage
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

/**
 * SMTP transport configured through `spring.mail.*`. The defaults target Google
 * Workspace / Gmail (`smtp.gmail.com:587` + app password); any other relay can
 * be used by overriding the standard Spring Mail properties.
 */
@Component
@ConditionalOnProperty(name = ["app.mail.provider"], havingValue = "smtp")
class SmtpMailGateway(
    private val mailSender: JavaMailSender,
    private val properties: MailProperties,
) : MailGateway {

    override fun send(message: MailMessage) {
        val mimeMessage: MimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")
        helper.setFrom(properties.from, properties.fromName)
        helper.setTo(message.to)
        helper.setSubject(message.subject)
        if (message.html != null) {
            helper.setText(message.text, message.html)
        } else {
            helper.setText(message.text)
        }
        mailSender.send(mimeMessage)
    }
}
