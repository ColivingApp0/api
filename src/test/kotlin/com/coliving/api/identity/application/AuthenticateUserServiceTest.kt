package com.coliving.api.identity.application

import com.coliving.api.identity.application.dto.LoginCommand
import com.coliving.api.identity.application.usecase.AuthenticateUserService
import com.coliving.api.identity.domain.enums.RoleCode
import com.coliving.api.identity.domain.enums.UserStatus
import com.coliving.api.identity.domain.model.Role
import com.coliving.api.identity.domain.model.User
import com.coliving.api.identity.domain.model.UserRole
import com.coliving.api.identity.domain.vo.Email
import com.coliving.api.identity.domain.vo.PasswordHash
import com.coliving.api.identity.infrastructure.config.IdentitySecurityProperties
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.UnauthorizedException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AuthenticateUserServiceTest {

    private val now: Instant = Instant.parse("2026-09-01T10:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    private val userRepository = FakeUserRepository()
    private val roleRepository = FakeRoleRepository()
    private val sessionStore = FakeSessionTokenStore()
    private val passwordHasher = FakePasswordHasher()
    private val tokenGenerator = FakeTokenGenerator(prefix = "s")
    private val properties = IdentitySecurityProperties()

    private val service = AuthenticateUserService(
        userRepository = userRepository,
        roleRepository = roleRepository,
        passwordHasher = passwordHasher,
        sessionTokenStore = sessionStore,
        tokenGenerator = tokenGenerator,
        properties = properties,
        clock = clock,
    )

    private fun activeUser(): User {
        val userId = UUID.randomUUID()
        val user = User.restore(
            id = userId,
            email = Email.of("user@example.com"),
            passwordHash = PasswordHash.of(passwordHasher.hash("Password123")),
            status = UserStatus.ACTIVE,
            emailVerified = true,
            createdAt = now,
            roles = emptySet(),
        )
        val role = roleRepository.findByCode(RoleCode.HUESPED_ESTUDIANTE)!!
        val userRole = UserRole.create(userId, role, now)
        val restored = User.restore(
            user.id,
            user.email,
            user.passwordHash,
            UserStatus.ACTIVE,
            true,
            user.createdAt,
            setOf(userRole),
        )
        userRepository.save(restored)
        return restored
    }

    @Test
    fun `valid credentials create a session stored as hash only`() {
        val user = activeUser()

        val result = service.login(
            LoginCommand(email = "user@example.com", password = "Password123", deviceLabel = "web"),
        )

        assertEquals(user.id, result.user.userId)
        assertEquals("s-1", result.token)
        assertEquals(now.plus(properties.sessionTtl), result.expiresAt)

        val stored = sessionStore.store.values.single()
        assertEquals(tokenGenerator.hash("s-1"), stored.tokenHash)
        assertEquals("web", stored.deviceLabel)
        assertEquals(null, stored.revokedAt)
    }

    @Test
    fun `active guest roles are reported in the login response`() {
        activeUser()

        val result = service.login(
            LoginCommand(email = "user@example.com", password = "Password123", deviceLabel = "web"),
        )

        assertEquals(listOf(RoleCode.HUESPED_ESTUDIANTE), result.user.activeRoles.map { it.code })
    }

    @Test
    fun `wrong password is rejected`() {
        activeUser()

        assertFailsWith<UnauthorizedException> {
            service.login(
                LoginCommand(email = "user@example.com", password = "WrongPass9", deviceLabel = "web"),
            )
        }
    }

    @Test
    fun `unknown email is rejected with the same generic error`() {
        assertFailsWith<UnauthorizedException> {
            service.login(
                LoginCommand(email = "ghost@example.com", password = "Password123", deviceLabel = "web"),
            )
        }
    }

    @Test
    fun `suspended user cannot log in`() {
        val suspended = User.restore(
            id = UUID.randomUUID(),
            email = Email.of("banned@example.com"),
            passwordHash = PasswordHash.of(passwordHasher.hash("Password123")),
            status = UserStatus.SUSPENDED,
            emailVerified = true,
            createdAt = now,
            roles = emptySet(),
        )
        userRepository.save(suspended)

        assertFailsWith<ForbiddenException> {
            service.login(
                LoginCommand(email = "banned@example.com", password = "Password123", deviceLabel = "web"),
            )
        }
    }
}