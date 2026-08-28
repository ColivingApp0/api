package com.coliving.api.identity.application

import com.coliving.api.identity.application.dto.RegisterUserCommand
import com.coliving.api.identity.application.usecase.RegisterUserService
import com.coliving.api.identity.domain.enums.SecurityTokenPurpose
import com.coliving.api.identity.domain.enums.UserStatus
import com.coliving.api.identity.infrastructure.config.IdentitySecurityProperties
import com.coliving.api.shared.error.ConflictException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RegisterUserServiceTest {

    private val now: Instant = Instant.parse("2026-09-01T10:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    private val userRepository = FakeUserRepository()
    private val securityTokenStore = FakeSecurityTokenStore()
    private val emailSender = FakeEmailSender()
    private val tokenGenerator = FakeTokenGenerator(prefix = "verify")
    private val passwordHasher = FakePasswordHasher()

    private val service = RegisterUserService(
        userRepository = userRepository,
        passwordHasher = passwordHasher,
        securityTokenStore = securityTokenStore,
        tokenGenerator = tokenGenerator,
        emailSender = emailSender,
        properties = IdentitySecurityProperties(),
        clock = clock,
    )

    @Test
    fun `register creates pending user, stores verification token and sends email`() {
        val result = service.register(
            RegisterUserCommand(email = " Ana@Example.com ", password = "Password123"),
        )

        assertEquals(UserStatus.PENDING_EMAIL_VERIFICATION, result.status)
        assertEquals("ana@example.com", userRepository.findById(result.userId)!!.email.value)
        assertTrue(!userRepository.findById(result.userId)!!.emailVerified)

        val storedHash = securityTokenStore.store.values.first().tokenHash
        assertTrue(storedHash.contains("verify"))

        assertEquals(1, emailSender.verificationEmails.size)
        assertEquals("ana@example.com", emailSender.verificationEmails.first().first.value)
        assertEquals("verify-1", emailSender.verificationEmails.first().second)
    }

    @Test
    fun `registering the same email twice fails`() {
        service.register(RegisterUserCommand(email = "x@example.com", password = "Password123"))

        assertFailsWith<ConflictException> {
            service.register(RegisterUserCommand(email = "X@example.com", password = "Password123"))
        }
    }

    @Test
    fun `short passwords are rejected`() {
        assertFailsWith<com.coliving.api.shared.error.InvalidArgumentException> {
            service.register(RegisterUserCommand(email = "y@example.com", password = "short"))
        }
    }

    @Test
    fun `verification token is stored for the created user`() {
        val result = service.register(
            RegisterUserCommand(email = "z@example.com", password = "Password123"),
        )

        val storedPurpose = securityTokenStore.store.values.first { it.userId == result.userId }
        assertEquals(SecurityTokenPurpose.EMAIL_VERIFICATION, storedPurpose.purpose)
    }
}