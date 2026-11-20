package com.coliving.api.identity.application

import com.coliving.api.identity.application.dto.ResetPasswordCommand
import com.coliving.api.identity.application.usecase.ResetPasswordService
import com.coliving.api.identity.domain.enums.SecurityTokenPurpose
import com.coliving.api.identity.domain.enums.UserStatus
import com.coliving.api.identity.domain.model.SecurityToken
import com.coliving.api.identity.domain.model.User
import com.coliving.api.identity.domain.vo.Email
import com.coliving.api.identity.domain.vo.PasswordHash
import com.coliving.api.identity.infrastructure.config.IdentitySecurityProperties
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResetPasswordServiceTest {

    private val now: Instant = Instant.parse("2026-09-01T10:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    private val userRepository = FakeUserRepository()
    private val securityTokenStore = FakeSecurityTokenStore()
    private val passwordHasher = FakePasswordHasher()
    private val tokenGenerator = FakeTokenGenerator(prefix = "reset")

    private val service = ResetPasswordService(
        securityTokenStore = securityTokenStore,
        userRepository = userRepository,
        passwordHasher = passwordHasher,
        tokenGenerator = tokenGenerator,
        clock = clock,
    )

    @Test
    fun `reset on a pending user promotes the account and verifies the email`() {
        val user = User.register(Email.of("pending@example.com"), PasswordHash.of("hash:old"), now)
        userRepository.save(user)

        val rawToken = "reset-1"
        securityTokenStore.save(
            SecurityToken.create(
                userId = user.id,
                purpose = SecurityTokenPurpose.PASSWORD_RESET,
                tokenHash = tokenGenerator.hash(rawToken),
                now = now,
                ttl = IdentitySecurityProperties().passwordResetTokenTtl,
            ),
        )

        service.reset(ResetPasswordCommand(token = rawToken, newPassword = "NewPassword123"))

        val saved = userRepository.findById(user.id)!!
        assertEquals(UserStatus.ACTIVE, saved.status)
        assertTrue(saved.emailVerified)
        assertEquals("hash:NewPassword123", saved.passwordHash.value)

        val stored = securityTokenStore.store.values.single { it.userId == user.id }
        assertEquals(now, stored.usedAt)
    }
}
