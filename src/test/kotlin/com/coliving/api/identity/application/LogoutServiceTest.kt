package com.coliving.api.identity.application

import com.coliving.api.identity.application.usecase.LogoutService
import com.coliving.api.identity.domain.model.SessionToken
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertTrue

class LogoutServiceTest {

    private val now: Instant = Instant.parse("2026-09-01T10:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    private val sessionStore = FakeSessionTokenStore()
    private val tokenGenerator = FakeTokenGenerator(prefix = "s")

    private val service = LogoutService(
        sessionTokenStore = sessionStore,
        tokenGenerator = tokenGenerator,
        clock = clock,
    )

    private fun addSession(rawToken: String): SessionToken {
        val session = SessionToken.create(
            userId = UUID.randomUUID(),
            tokenHash = tokenGenerator.hash(rawToken),
            deviceLabel = "mobile",
            now = now,
            ttl = Duration.ofDays(30),
        )
        sessionStore.save(session)
        return session
    }

    @Test
    fun `logout revokes only the target session`() {
        val a = addSession("a-1")
        addSession("a-2")

        service.logout("a-1")

        assertTrue(sessionStore.findByHash(tokenGenerator.hash("a-1"))!!.revokedAt != null)
        assertTrue(sessionStore.findByHash(tokenGenerator.hash("a-2"))!!.revokedAt == null)
        assertTrue(a.id == a.id)
    }

    @Test
    fun `logout with unknown token is idempotent`() {
        service.logout("does-not-exist")
    }

    @Test
    fun `blank token is ignored`() {
        addSession("a-3")
        service.logout("   ")
        assertTrue(sessionStore.findByHash(tokenGenerator.hash("a-3"))!!.revokedAt == null)
    }
}