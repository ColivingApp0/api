package com.coliving.api.messaging.application.usecase

import com.coliving.api.messaging.application.dto.PushTokenView
import com.coliving.api.messaging.application.dto.RegisterPushTokenCommand
import com.coliving.api.messaging.domain.model.PushToken
import com.coliving.api.messaging.domain.repository.PushTokenRepository
import java.time.Clock
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Registration of FCM device tokens (RF-051). The token is unique: if the same
 * installation signs in with another account it is reassigned, and repeated
 * registrations only refresh `lastSeenAt`.
 */
@Service
class PushTokenService(
    private val pushTokenRepository: PushTokenRepository,
    private val clock: Clock,
) {

    @Transactional
    fun register(userId: UUID, command: RegisterPushTokenCommand): PushTokenView {
        val now = clock.instant()
        val normalized = command.token.trim()
        val existing = pushTokenRepository.findByToken(normalized)
        val token = when {
            existing == null -> PushToken.create(
                id = UUID.randomUUID(),
                userId = userId,
                token = normalized,
                platform = command.platform,
                now = now,
            )
            existing.userId == userId -> existing.touch(command.platform, now)
            else -> existing.reassign(userId, command.platform, now)
        }
        pushTokenRepository.save(token)
        return token.toView()
    }

    /** Idempotent: an unknown or foreign token is silently ignored. */
    @Transactional
    fun unregister(userId: UUID, rawToken: String) {
        val token = pushTokenRepository.findByToken(rawToken.trim()) ?: return
        if (token.userId == userId) {
            pushTokenRepository.delete(token)
        }
    }
}

internal fun PushToken.toView(): PushTokenView =
    PushTokenView(
        id = id,
        platform = platform,
        createdAt = createdAt,
        lastSeenAt = lastSeenAt,
    )
