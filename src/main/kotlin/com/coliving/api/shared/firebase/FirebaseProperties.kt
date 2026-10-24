package com.coliving.api.shared.firebase

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Firebase Admin configuration bound from `app.firebase.*`.
 *
 * Credentials are never committed: provide the service-account JSON through
 * `FIREBASE_CREDENTIALS` (absolute path) or use Application Default
 * Credentials (`gcloud auth application-default login` / workload identity).
 */
@ConfigurationProperties(prefix = "app.firebase")
data class FirebaseProperties(
    val enabled: Boolean = false,
    val projectId: String? = null,
    val credentialsPath: String? = null,
)
