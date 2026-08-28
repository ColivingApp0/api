package com.coliving.api.identity.application.usecase

import com.coliving.api.identity.application.dto.SelectRoleCommand
import com.coliving.api.identity.application.dto.SelectRoleResult
import com.coliving.api.identity.application.dto.UserRoleSummary
import com.coliving.api.identity.application.port.out.ProfileCompletenessPort
import com.coliving.api.identity.domain.enums.RoleCode
import com.coliving.api.identity.domain.repository.RoleRepository
import com.coliving.api.identity.domain.repository.UserRepository
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.NotFoundException
import java.time.Clock
import org.springframework.stereotype.Service

/**
 * RF-004: the user picks an initial role (guest or host) and may change it,
 * but role changes require the profile data relevant to the target role.
 * The completeness rule is owned by the `profile` bounded context and is
 * reached through [ProfileCompletenessPort]; identity only enforces the flow.
 */
@Service
class SelectRoleService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val profileCompletenessPort: ProfileCompletenessPort,
    private val clock: Clock,
) {

    /** Roles a user may select on their own; others are assigned by admins. */
    private val userSelectableRoles = setOf(
        RoleCode.HUESPED_ESTUDIANTE,
        RoleCode.HUESPED_TURISTA,
        RoleCode.ANFITRION,
    )

    fun selectRole(command: SelectRoleCommand): SelectRoleResult {
        if (command.roleCode !in userSelectableRoles) {
            throw ForbiddenException("Role ${command.roleCode} cannot be selected by the user")
        }

        val user = userRepository.findById(command.userId)
            ?: throw NotFoundException("User not found")
        if (!user.emailVerified) {
            throw ConflictException("E-mail must be verified before selecting a role")
        }
        if (!user.isActive()) throw ForbiddenException("Account is not active")

        val role = roleRepository.findByCode(command.roleCode)
            ?: throw NotFoundException("Role ${command.roleCode} not found")
        if (!role.active) throw ConflictException("Role ${command.roleCode} is inactive")

        if (!profileCompletenessPort.hasMinimumDataFor(user.id, role.code)) {
            throw ConflictException("Profile is incomplete for role ${role.code}")
        }

        val now = clock.instant()
        val userRole = user.assignRole(role, now)
        userRepository.save(user)
        return SelectRoleResult(
            userId = user.id,
            activeRole = UserRoleSummary(
                id = userRole.id,
                roleId = userRole.roleId,
                code = userRole.roleCode,
                name = role.name,
                active = true,
                assignedAt = userRole.assignedAt,
            ),
        )
    }
}