package com.coliving.api.messaging.application

import com.coliving.api.messaging.application.dto.RegisterPushTokenCommand
import com.coliving.api.messaging.application.usecase.PushTokenService
import com.coliving.api.messaging.domain.enums.PushPlatform
import com.coliving.api.shared.error.InvalidArgumentException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PushTokenServiceTest {

    private val now: Instant = Instant.parse("2026-10-28T10:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    private val repository = FakePushTokenRepository()
    private val service = PushTokenService(repository, clock)
    private val userId = UUID.randomUUID()

    @Test
    fun `registers the token of the current device`() {
        val view = service.register(
            userId,
            RegisterPushTokenCommand(token = "  token-1  ", platform = PushPlatform.WEB),
        )

        assertEquals(PushPlatform.WEB, view.platform)
        assertEquals(now, view.createdAt)
        assertEquals(now, view.lastSeenAt)
        assertEquals(userId, repository.findByToken("token-1")?.userId)
    }

    @Test
    fun `re-registering the same device refreshes it instead of duplicating`() {
        service.register(userId, RegisterPushTokenCommand("token-1", PushPlatform.WEB))

        service.register(userId, RegisterPushTokenCommand("token-1", PushPlatform.ANDROID))

        assertEquals(1, repository.findByUser(userId).size)
        assertEquals(PushPlatform.ANDROID, repository.findByToken("token-1")?.platform)
    }

    @Test
    fun `a token used by another account is reassigned to the new owner`() {
        val other = UUID.randomUUID()
        service.register(other, RegisterPushTokenCommand("shared", PushPlatform.WEB))

        service.register(userId, RegisterPushTokenCommand("shared", PushPlatform.WEB))

        assertTrue(repository.findByUser(other).isEmpty())
        assertEquals(userId, repository.findByToken("shared")?.userId)
    }

    @Test
    fun `rejects blank tokens`() {
        assertFailsWith<InvalidArgumentException> {
            service.register(userId, RegisterPushTokenCommand("   ", PushPlatform.WEB))
        }
    }

    @Test
    fun `unregister is idempotent and only removes the owner's token`() {
        service.register(userId, RegisterPushTokenCommand("token-1", PushPlatform.WEB))

        service.unregister(UUID.randomUUID(), "token-1")
        assertEquals(userId, repository.findByToken("token-1")?.userId)

        service.unregister(userId, "token-1")
        assertNull(repository.findByToken("token-1"))

        // Unknown token: no error.
        service.unregister(userId, "token-1")
    }
}
