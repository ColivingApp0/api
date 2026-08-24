package com.coliving.api.identity.application.usecase

import com.coliving.api.identity.application.port.out.TokenGenerator
import com.coliving.api.identity.domain.repository.SessionTokenStore
import java.time.Clock
import org.springframework.stereotype.Service

/**
 * RF-002: closing the session revokes the token of the corresponding device.
 * Idempotent: revoking an unknown/expired token is a no-op.
 */
@Service
class LogoutService(
    private val sessionTokenStore: SessionTokenStore,
    private val tokenGenerator: TokenGenerator,
    private val clock: Clock,
) {

    fun logout(rawToken: String) {
        if (rawToken.isBlank()) return
        sessionTokenStore.revokeByHash(tokenGenerator.hash(rawToken), clock.instant())
    }
}