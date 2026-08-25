package com.coliving.api.identity.domain

import com.coliving.api.identity.domain.enums.RoleCode
import com.coliving.api.identity.domain.enums.UserStatus
import com.coliving.api.identity.domain.model.Role
import com.coliving.api.identity.domain.model.User
import com.coliving.api.identity.domain.vo.Email
import com.coliving.api.identity.domain.vo.PasswordHash
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserTest {

    private val now: Instant = Instant.parse("2026-09-01T10:00:00Z")

    private fun newUser(
        emailVerified: Boolean = false,
        status: UserStatus = UserStatus.ACTIVE,
    ): User = User.restore(
        id = UUID.randomUUID(),
        email = Email.of("user@example.com"),
        passwordHash = PasswordHash.of("hash:secret"),
        status = status,
        emailVerified = emailVerified,
        createdAt = now,
        roles = emptySet(),
    )

    @Test
    fun `registered user starts pending verification and inactive`() {
        val user = User.register(Email.of("a@b.co"), PasswordHash.of("hash:x"), now)

        assertEquals(UserStatus.PENDING_EMAIL_VERIFICATION, user.status)
        assertFalse(user.emailVerified)
        assertFalse(user.isActive())
    }

    @Test
    fun `verify email promotes account to active`() {
        val user = newUser(emailVerified = false, status = UserStatus.PENDING_EMAIL_VERIFICATION)

        assertTrue(user.verifyEmail())
        assertTrue(user.emailVerified)
        assertEquals(UserStatus.ACTIVE, user.status)
        assertTrue(user.isActive())
    }

    @Test
    fun `assigning a second role deactivates the previous one`() {
        val user = newUser(emailVerified = true)
        val guest = Role.of(UUID.randomUUID(), RoleCode.HUESPED_ESTUDIANTE, "Guest")
        val host = Role.of(UUID.randomUUID(), RoleCode.ANFITRION, "Host")

        user.assignRole(guest, now)
        user.assignRole(host, now)

        assertTrue(user.hasActiveRole(RoleCode.ANFITRION))
        assertFalse(user.hasActiveRole(RoleCode.HUESPED_ESTUDIANTE))
        assertEquals(setOf(RoleCode.ANFITRION), user.activeRoleCodes())
    }

    @Test
    fun `re-assigning the active role keeps it active`() {
        val user = newUser(emailVerified = true)
        val guest = Role.of(UUID.randomUUID(), RoleCode.HUESPED_ESTUDIANTE, "Guest")

        val first = user.assignRole(guest, now)
        val second = user.assignRole(guest, now.plusSeconds(60))

        assertEquals(first.id, second.id)
        assertTrue(user.hasActiveRole(RoleCode.HUESPED_ESTUDIANTE))
    }

    @Test
    fun `inactive role cannot be assigned`() {
        val user = newUser(emailVerified = true)
        val inactive = Role.of(UUID.randomUUID(), RoleCode.RESIDENTE, "Resident", active = false)

        val error = kotlin.runCatching { user.assignRole(inactive, now) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }
}