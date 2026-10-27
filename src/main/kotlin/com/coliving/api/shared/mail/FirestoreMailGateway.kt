package com.coliving.api.shared.mail

import com.google.cloud.firestore.Firestore
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * Firebase transport: writes the message to a Firestore collection that the
 * official "Trigger Email from Firestore" extension watches. Requires
 * `app.firebase.enabled=true` plus valid Google credentials.
 *
 * Document shape expected by the extension:
 * https://firebase.google.com/products/extensions/firebase-firestore-send-email
 */
@Component
@ConditionalOnProperty(name = ["app.mail.provider"], havingValue = "firebase")
class FirestoreMailGateway(
    private val firestore: Firestore,
    private val properties: MailProperties,
) : MailGateway {

    override fun send(message: MailMessage) {
        val document = mapOf(
            "to" to listOf(message.to),
            "message" to mapOf(
                "subject" to message.subject,
                "text" to message.text,
                "html" to (message.html ?: message.text),
            ),
        )
        firestore.collection(properties.firebaseCollection).add(document).get()
    }
}
