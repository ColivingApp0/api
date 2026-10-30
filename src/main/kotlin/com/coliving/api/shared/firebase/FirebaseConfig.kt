package com.coliving.api.shared.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.google.firebase.messaging.FirebaseMessaging
import java.io.FileInputStream
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Wires the Firebase Admin SDK only when `app.firebase.enabled=true`. Both the
 * Firestore mail transport and FCM push depend on these beans, so a deployment
 * that does not use Firebase never tries to load credentials.
 */
@Configuration
@EnableConfigurationProperties(FirebaseProperties::class)
@ConditionalOnProperty(name = ["app.firebase.enabled"], havingValue = "true")
class FirebaseConfig {

    private val logger = LoggerFactory.getLogger(FirebaseConfig::class.java)

    @Bean
    fun firebaseApp(properties: FirebaseProperties): FirebaseApp {
        FirebaseApp.getApps().firstOrNull()?.let { return it }

        val credentials = loadCredentials(properties)
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .apply {
                properties.projectId?.takeIf { it.isNotBlank() }?.let { setProjectId(it) }
            }
            .build()
        val app = FirebaseApp.initializeApp(options)
        logger.info("Firebase Admin initialized for project {}", app.options.projectId)
        return app
    }

    @Bean
    fun firestore(firebaseApp: FirebaseApp): Firestore =
        FirestoreClient.getFirestore(firebaseApp)

    @Bean
    fun firebaseMessaging(firebaseApp: FirebaseApp): FirebaseMessaging =
        FirebaseMessaging.getInstance(firebaseApp)

    private fun loadCredentials(properties: FirebaseProperties): GoogleCredentials {
        val path = properties.credentialsPath?.takeIf { it.isNotBlank() }
            ?: return GoogleCredentials.getApplicationDefault()
        return try {
            FileInputStream(path).use { GoogleCredentials.fromStream(it) }
        } catch (error: Exception) {
            throw IllegalStateException(
                "Cannot load Firebase credentials from '$path'. Check FIREBASE_CREDENTIALS.",
                error,
            )
        }
    }
}
