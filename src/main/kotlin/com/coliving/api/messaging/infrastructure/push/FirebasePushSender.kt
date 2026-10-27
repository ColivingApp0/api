package com.coliving.api.messaging.infrastructure.push

import com.coliving.api.messaging.application.port.out.PushSender
import com.coliving.api.messaging.domain.model.PushToken
import com.coliving.api.messaging.domain.repository.PushTokenRepository
import com.coliving.api.shared.mail.MailProperties
import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.google.firebase.messaging.WebpushConfig
import com.google.firebase.messaging.WebpushFcmOptions
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * FCM push for new messages (RF-051). The payload is content-free: a generic
 * title/body plus the conversation id, so a locked-screen preview never exposes
 * the message or reservation details.
 */
@Component
@ConditionalOnProperty(name = ["app.push.enabled"], havingValue = "true")
class FirebasePushSender(
    private val firebaseMessaging: FirebaseMessaging,
    private val pushTokens: PushTokenRepository,
    private val properties: MailProperties,
) : PushSender {

    private val logger = LoggerFactory.getLogger(FirebasePushSender::class.java)

    override fun sendNewMessage(userId: UUID, conversationId: UUID) {
        val tokens = pushTokens.findByUser(userId)
        if (tokens.isEmpty()) {
            return
        }
        tokens.chunked(MAX_TOKENS_PER_REQUEST).forEach { batch ->
            sendBatch(batch, conversationId)
        }
    }

    private fun sendBatch(tokens: List<PushToken>, conversationId: UUID) {
        val message = MulticastMessage.builder()
            .addAllTokens(tokens.map { it.token })
            .setNotification(
                Notification.builder()
                    .setTitle(NEW_MESSAGE_TITLE)
                    .setBody(NEW_MESSAGE_BODY)
                    .build(),
            )
            .putData("type", "MENSAJE_NUEVO")
            .putData("conversationId", conversationId.toString())
            .setWebpushConfig(
                WebpushConfig.builder()
                    .setFcmOptions(
                        WebpushFcmOptions.withLink(
                            "${properties.publicBaseUrl.trimEnd('/')}/mensajes/$conversationId",
                        ),
                    )
                    .build(),
            )
            .build()

        val response = firebaseMessaging.sendEachForMulticast(message)
        removeInvalidTokens(tokens, response)
    }

    private fun removeInvalidTokens(tokens: List<PushToken>, response: BatchResponse) {
        response.responses.forEachIndexed { index, sendResponse ->
            if (sendResponse.isSuccessful) {
                return@forEachIndexed
            }
            val errorCode = sendResponse.exception?.messagingErrorCode
            if (errorCode == MessagingErrorCode.UNREGISTERED ||
                errorCode == MessagingErrorCode.INVALID_ARGUMENT
            ) {
                runCatching { pushTokens.delete(tokens[index]) }
                    .onFailure { logger.warn("Could not delete invalid push token", it) }
            } else {
                logger.warn(
                    "FCM send failed for push token {}: {}",
                    tokens[index].id,
                    sendResponse.exception?.message,
                )
            }
        }
    }

    companion object {
        const val NEW_MESSAGE_TITLE = "Tienes un nuevo mensaje"
        const val NEW_MESSAGE_BODY = "Abre ColivinApp para leerlo."
        private const val MAX_TOKENS_PER_REQUEST = 500
    }
}
