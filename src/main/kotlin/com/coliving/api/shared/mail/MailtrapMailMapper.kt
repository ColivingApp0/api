package com.coliving.api.shared.mail

import io.mailtrap.model.request.emails.Address
import io.mailtrap.model.request.emails.MailtrapMail

/**
 * Pure mapping from the application's [MailMessage] to the Mailtrap SDK model.
 * Kept separate from the HTTP client so the payload can be unit-tested.
 */
internal object MailtrapMailMapper {

    fun toMail(message: MailMessage, properties: MailProperties): MailtrapMail {
        val builder = MailtrapMail.builder()
            .from(Address(properties.from, properties.fromName))
            .to(listOf(Address(message.to)))
            .subject(message.subject)
            .text(message.text)
            .category(message.category)
        message.html?.let { builder.html(it) }
        return builder.build()
    }
}
