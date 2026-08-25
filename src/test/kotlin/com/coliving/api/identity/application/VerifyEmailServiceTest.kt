package com.coliving.api.identity.application

import com.coliving.api.identity.application.dto.VerifyEmailCommand
import com.coliving.api.identity.application.usecase.VerifyEmailService
import com.coliving.api.identity.domain.enums.SecurityTokenPurpose
import com.coliving.api.identity.domain.model.SecurityToken
import com.coliving.api.identity.domain.model.User
import com.coliving.api.identity.domain.vo.Email
import com.coliving.api.identity.domain.vo.PasswordHash
import com.coliving.api.identity.infrastructure.config.IdentitySecurityProperties
import com.coliving.api.shared.error.InvalidArgumentException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VerifyEmailServiceTest {

    private val now: Instant = Instant.parse("2026-09-01T10:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    private val userRepository = FakeUserRepository()
    private val securityTokenStore = FakeSecurityTokenStore()
    private val tokenGenerator = FakeTokenGenerator(prefix = "verify")

    private val service = VerifyEmailService(
        securityTokenStore = securityTokenStore,
        userRepository = userRepository,
        tokenGenerator = tokenGenerator,
        clock = clock,
    )

    @Test
    fun `valid token verifies the email and consumes the token`() {
        val user = User.register(
            Email.of("a@example.com"),
            PasswordHash.of("hash:x"),
            now,
        )
        userRepository.save(user)

        val rawToken = "verify-1"
        securityTokenStore.save(
            SecurityToken.create(
                userId = user.id,
                purpose = SecurityTokenPurpose.EMAIL_VERIFICATION,
                tokenHash = tokenGenerator.hash(rawToken),
                now = now,
                ttl = IdentitySecurityProperties().emailVerificationTokenTtl,
            ),
        )

        service.verify(VerifyEmailCommand(token = rawToken))

        val saved = userRepository.findById(user.id)!!
        assertTrue(saved.emailVerified)
        assertEquals(com.coliving.api.identity.domain.enums.UserStatus.ACTIVE, saved.status)
        assertTrue(securityTokenStore.store.values.first { it.userId == user.id }.usedAt != null)
    }

    @Test
    fun `unknown token is rejected without side effects`() {
        assertFailsWith<InvalidArgumentException> {
            service.verify(VerifyEmailCommand(token = "verify-999"))
        }
    }

    @Test
    fun `token cannot be reused`() {
        val user = User.register(Email.of("b@example.com"), PasswordHash.of("hash:x"), now)
        userRepository.save(user)
        val rawToken = "reuse-me"
        securityTokenStore.save(
            SecurityToken.create(
                userId = user.id,
                purpose = SecurityTokenPurpose.EMAIL_VERIFICATION,
                tokenHash = tokenGenerator.hash(rawToken),
                now = now,
                ttl = Duration.ofHours(24),
            ),
        )

        service.verify(VerifyEmailCommand(token = rawToken))

        assertFailsWith<com.coliving.api.shared.error.ConflictException> {
            service.verify(VerifyEmailCommand(token = rawToken))
        }
    }
}