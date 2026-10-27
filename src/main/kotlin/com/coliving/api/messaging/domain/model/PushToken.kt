package com.coliving.api.messaging.domain.model

import com.coliving.api.messaging.domain.enums.PushPlatform
import com.coliving.api.shared.error.InvalidArgumentException
import java.time.Instant
import java.util.UUID

/**
 * FCM registration token of one device (RF-051 external push channel).
 *
 * A token identifies a browser/app installation, not a person: when the same
 * device signs in with another account the token is reassigned instead of
 * duplicated. The raw token is stored because FCM needs it to address the
 * device; it is never returned to clients after registration.
 */
class PushToken(
    val id: UUID,
    val userId: UUID,
    val token: String,
    val platform: PushPlatform,
    val createdAt: Instant,
    val lastSeenAt: Instant,
) {

    fun touch(platform: PushPlatform, now: Instant): PushToken =
        PushToken(id, userId, token, platform, createdAt, now)

    fun reassign(userId: UUID, platform: PushPlatform, now: Instant): PushToken =
        PushToken(id, userId, token, platform, createdAt, now)

    companion object {
        const val MAX_TOKEN_LENGTH = 512

        fun create(
            id: UUID,
            userId: UUID,
            token: String,
            platform: PushPlatform,
            now: Instant,
        ): PushToken {
            val normalized = token.trim()
            if (normalized.isEmpty()) {
                throw InvalidArgumentException("Push token must not be empty")
            }
            if (normalized.length > MAX_TOKEN_LENGTH) {
                throw InvalidArgumentException(
                    "Push token must not exceed $MAX_TOKEN_LENGTH characters",
                )
            }
            return PushToken(
                id = id,
                userId = userId,
                token = normalized,
                platform = platform,
                createdAt = now,
                lastSeenAt = now,
            )
        }
    }
}
