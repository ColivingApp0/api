package com.coliving.api.messaging.application.dto

import com.coliving.api.messaging.domain.enums.PushPlatform
import java.time.Instant
import java.util.UUID

/** Registers the FCM token of the current device (RF-051). */
data class RegisterPushTokenCommand(
    val token: String,
    val platform: PushPlatform,
)

/** Public projection: the raw token is never returned after registration. */
data class PushTokenView(
    val id: UUID,
    val platform: PushPlatform,
    val createdAt: Instant,
    val lastSeenAt: Instant,
)
