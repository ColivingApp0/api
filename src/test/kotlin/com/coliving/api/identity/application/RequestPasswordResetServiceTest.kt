package com.coliving.api.identity.application

import com.coliving.api.identity.application.dto.RequestPasswordResetCommand
import com.coliving.api.identity.application.usecase.RequestPasswordResetService
import com.coliving.api.identity.domain.enums.SecurityTokenPurpose
import com.coliving.api.identity.domain.model.User
import com.coliving.api.identity.domain.vo.Email
import com.coliving.api.identity.domain.vo.PasswordHash
import com.coliving.api.identity.infrastructure.config.IdentitySecurityProperties
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RequestPasswordResetServiceTest {

    private val now: Instant = Instant.parse("2026-09-01T10:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    private val userRepository = FakeUserRepository()
    private val securityTokenStore = FakeSecurityTokenStore()
    private val emailSender = FakeEmailSender()
    private val tokenGenerator = FakeTokenGenerator(prefix = "reset")

    private val service = RequestPasswordResetService(
        userRepository = userRepository,
        securityTokenStore = securityTokenStore,
        tokenGenerator = tokenGenerator,
        emailSender = emailSender,
        properties = IdentitySecurityProperties(),
        clock = clock,
    )

    @Test
    fun `pending user receives the email and a password reset token is stored`() {
        val user = User.register(Email.of("pending@example.com"), PasswordHash.of("hash:x"), now)
        userRepository.save(user)

        service.request(RequestPasswordResetCommand(email = "pending@example.com"))

        assertEquals(1, emailSender.passwordResetEmails.size)
        assertEquals("pending@example.com", emailSender.passwordResetEmails.single().first.value)
        assertEquals("reset-1", emailSender.passwordResetEmails.single().second)

        val stored = securityTokenStore.store.values.single { it.userId == user.id }
        assertEquals(SecurityTokenPurpose.PASSWORD_RESET, stored.purpose)
        assertEquals(tokenGenerator.hash("reset-1"), stored.tokenHash)
        assertNull(stored.usedAt)
    }

    @Test
    fun `suspended user receives no email and no token is stored`() {
        val user = User.register(Email.of("blocked@example.com"), PasswordHash.of("hash:x"), now)
        user.suspend()
        userRepository.save(user)

        service.request(RequestPasswordResetCommand(email = "blocked@example.com"))

        assertTrue(emailSender.passwordResetEmails.isEmpty())
        assertTrue(securityTokenStore.store.isEmpty())
    }

    @Test
    fun `unknown email does not throw and sends nothing`() {
        service.request(RequestPasswordResetCommand(email = "nobody@example.com"))

        assertTrue(emailSender.passwordResetEmails.isEmpty())
        assertTrue(securityTokenStore.store.isEmpty())
    }
}
