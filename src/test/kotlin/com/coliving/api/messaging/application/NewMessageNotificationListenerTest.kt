package com.coliving.api.messaging.application

import com.coliving.api.messaging.application.event.NewMessageEvent
import com.coliving.api.messaging.application.port.out.RecipientContact
import com.coliving.api.messaging.application.usecase.MessageEmailNotifier
import com.coliving.api.messaging.application.usecase.NewMessageNotificationListener
import com.coliving.api.shared.mail.MailGateway
import com.coliving.api.shared.mail.MailMessage
import com.coliving.api.shared.mail.MailProperties
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NewMessageNotificationListenerTest {

    private val push = FakePushSender()
    private val contacts = FakeRecipientContactPort()
    private val mail = CapturingMailGateway()
    private val properties = MailProperties(
        publicBaseUrl = "https://app.coliving.test",
        messageNotificationsEnabled = true,
    )
    private val notifier = MessageEmailNotifier(contacts, mail, properties)
    private val listener = NewMessageNotificationListener(push, notifier)

    private val userId = UUID.randomUUID()
    private val conversationId = UUID.randomUUID()

    @Test
    fun `fans out a new message to push and e-mail`() {
        contacts.contacts[userId] = RecipientContact(userId, "ada@example.com")

        listener.onNewMessage(NewMessageEvent(userId, conversationId))

        assertEquals(listOf(userId to conversationId), push.sent)
        val message = mail.sent.single()
        assertEquals("ada@example.com", message.to)
        assertTrue(message.text.contains("https://app.coliving.test/mensajes/$conversationId"))
    }

    @Test
    fun `skips e-mail when message notifications are disabled`() {
        contacts.contacts[userId] = RecipientContact(userId, "ada@example.com")
        val disabled = MessageEmailNotifier(
            contacts,
            mail,
            properties.copy(messageNotificationsEnabled = false),
        )
        val listener = NewMessageNotificationListener(push, disabled)

        listener.onNewMessage(NewMessageEvent(userId, conversationId))

        assertEquals(1, push.sent.size)
        assertTrue(mail.sent.isEmpty())
    }

    @Test
    fun `still pushes when the recipient has no contact`() {
        listener.onNewMessage(NewMessageEvent(userId, conversationId))

        assertEquals(1, push.sent.size)
        assertTrue(mail.sent.isEmpty())
    }
}

class CapturingMailGateway : MailGateway {
    val sent = mutableListOf<MailMessage>()

    override fun send(message: MailMessage) {
        sent.add(message)
    }
}
