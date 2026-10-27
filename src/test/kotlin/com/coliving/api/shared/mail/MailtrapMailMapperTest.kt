package com.coliving.api.shared.mail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MailtrapMailMapperTest {

    private val properties = MailProperties(
        from = "hello@demomailtrap.co",
        fromName = "ColivinApp",
    )

    @Test
    fun `maps the shared message to the Mailtrap SDK model`() {
        val mail = MailtrapMailMapper.toMail(
            MailMessage(
                to = "ada@example.com",
                subject = "Verifica tu correo",
                text = "Hola",
                html = "<p>Hola</p>",
                category = "account",
            ),
            properties,
        )

        assertEquals("hello@demomailtrap.co", mail.from.email)
        assertEquals("ColivinApp", mail.from.name)
        assertEquals("ada@example.com", mail.to.single().email)
        assertEquals("Verifica tu correo", mail.subject)
        assertEquals("Hola", mail.text)
        assertEquals("<p>Hola</p>", mail.html)
        assertEquals("account", mail.category)
    }

    @Test
    fun `omits the html body when the message has none`() {
        val mail = MailtrapMailMapper.toMail(
            MailMessage(to = "ada@example.com", subject = "Asunto", text = "Texto"),
            properties,
        )

        assertNull(mail.html)
        assertEquals("ColivinApp", mail.category)
    }
}
