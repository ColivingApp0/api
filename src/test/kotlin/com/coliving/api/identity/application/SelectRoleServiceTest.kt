package com.coliving.api.identity.application

import com.coliving.api.identity.application.dto.SelectRoleCommand
import com.coliving.api.identity.application.usecase.SelectRoleService
import com.coliving.api.identity.domain.enums.RoleCode
import com.coliving.api.identity.domain.enums.UserStatus
import com.coliving.api.identity.domain.model.User
import com.coliving.api.identity.domain.vo.Email
import com.coliving.api.identity.domain.vo.PasswordHash
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SelectRoleServiceTest {

    private val now: Instant = Instant.parse("2026-09-01T10:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    private val userRepository = FakeUserRepository()
    private val roleRepository = FakeRoleRepository()
    private val profileCompletenessPort = FakeProfileCompletenessPort()

    private val service = SelectRoleService(
        userRepository = userRepository,
        roleRepository = roleRepository,
        profileCompletenessPort = profileCompletenessPort,
        clock = clock,
    )

    private fun verifiedUser(userId: UUID = UUID.randomUUID()): User {
        val user = User.restore(
            id = userId,
            email = Email.of("guest@example.com"),
            passwordHash = PasswordHash.of("hash:x"),
            status = UserStatus.ACTIVE,
            emailVerified = true,
            createdAt = now,
            roles = emptySet(),
        )
        userRepository.save(user)
        return user
    }

    @Test
    fun `verified user with complete guest profile selects a guest role`() {
        val user = verifiedUser()
        profileCompletenessPort.complete(user.id, RoleCode.HUESPED_ESTUDIANTE)

        val result = service.selectRole(
            SelectRoleCommand(userId = user.id, roleCode = RoleCode.HUESPED_ESTUDIANTE),
        )

        assertEquals(RoleCode.HUESPED_ESTUDIANTE, result.activeRole.code)
        assertTrue(userRepository.findById(user.id)!!.hasActiveRole(RoleCode.HUESPED_ESTUDIANTE))
    }

    @Test
    fun `changing the active role requires profile data of the target role`() {
        val user = verifiedUser()
        // Guest profile without a phone number: enough to be a guest,
        // not enough to become an ANFITRION (name + phone required by the
        // profile context, enforced through ProfileCompletenessPort).
        profileCompletenessPort.complete(user.id, RoleCode.HUESPED_TURISTA)
        service.selectRole(SelectRoleCommand(userId = user.id, roleCode = RoleCode.HUESPED_TURISTA))

        assertFailsWith<ConflictException> {
            service.selectRole(SelectRoleCommand(userId = user.id, roleCode = RoleCode.ANFITRION))
        }
    }

    @Test
    fun `unverified users cannot select a role`() {
        val user = User.restore(
            id = UUID.randomUUID(),
            email = Email.of("draft@example.com"),
            passwordHash = PasswordHash.of("hash:x"),
            status = UserStatus.PENDING_EMAIL_VERIFICATION,
            emailVerified = false,
            createdAt = now,
            roles = emptySet(),
        )
        userRepository.save(user)

        assertFailsWith<ConflictException> {
            service.selectRole(
                SelectRoleCommand(userId = user.id, roleCode = RoleCode.HUESPED_ESTUDIANTE),
            )
        }
    }

    @Test
    fun `moderator role cannot be selected by the user`() {
        val user = verifiedUser()
        profileCompletenessPort.complete(user.id, RoleCode.HUESPED_ESTUDIANTE)

        assertFailsWith<ForbiddenException> {
            service.selectRole(
                SelectRoleCommand(userId = user.id, roleCode = RoleCode.MODERADOR),
            )
        }
    }
}