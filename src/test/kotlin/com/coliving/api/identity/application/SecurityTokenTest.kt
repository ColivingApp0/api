package com.coliving.api.identity.application

import com.coliving.api.identity.domain.enums.SecurityTokenPurpose
import com.coliving.api.identity.domain.model.SecurityToken
import com.coliving.api.shared.error.ConflictException
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SecurityTokenTest {

    private val now: Instant = Instant.parse("2026-09-01T10:00:00Z")

    private fun token(): SecurityToken = SecurityToken.create(
        userId = UUID.randomUUID(),
        purpose = SecurityTokenPurpose.PASSWORD_RESET,
        tokenHash = "sha256(raw)",
        now = now,
        ttl = Duration.ofMinutes(30),
    )

    @Test
    fun `token is consumed exactly once`() {
        val token = token()

        token.consume(now.plusSeconds(1))
        assertFailsWith<ConflictException> {
            token.consume(now.plusSeconds(2))
        }
    }

    @Test
    fun `expired token cannot be consumed`() {
        val token = token()

        assertFailsWith<ConflictException> {
            token.consume(now.plus(Duration.ofMinutes(31)))
        }
    }

    @Test
    fun `session token expires and revokes`() {
        val session = com.coliving.api.identity.domain.model.SessionToken.create(
            userId = UUID.randomUUID(),
            tokenHash = "sha256(x)",
            deviceLabel = "web",
            now = now,
            ttl = Duration.ofDays(30),
        )

        assertTrue(session.isActive(now.plusSeconds(1)))
        session.revoke(now.plusSeconds(5))
        assertTrue(!session.isActive(now.plusSeconds(6)))
        assertTrue(session.isExpired(now.plus(Duration.ofDays(31))))
    }
}