package com.coliving.api.identity.application.usecase

import com.coliving.api.identity.application.dto.AssignRoleCommand
import com.coliving.api.identity.application.dto.RoleSummary
import com.coliving.api.identity.application.dto.SelectRoleResult
import com.coliving.api.identity.application.dto.UserRoleSummary
import com.coliving.api.identity.domain.repository.RoleRepository
import com.coliving.api.identity.domain.repository.UserRepository
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.NotFoundException
import java.time.Clock
import org.springframework.stereotype.Service

/**
 * Admin operation: assigns any role (including MODERADOR / ADMINISTRADOR) to a
 * user. Unlike user-driven role selection, the profile-completeness check is
 * deliberately bypassed — assignment is an administrative decision.
 */
@Service
class AssignRoleService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val clock: Clock,
) {

    fun assign(command: AssignRoleCommand): SelectRoleResult {
        val user = userRepository.findById(command.userId)
            ?: throw NotFoundException("User not found")
        if (!user.isActive()) throw ForbiddenException("Account is not active")

        val role = roleRepository.findByCode(command.roleCode)
            ?: throw NotFoundException("Role ${command.roleCode} not found")
        if (!role.active) throw ConflictException("Role ${command.roleCode} is inactive")

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

    fun listRoles(): List<RoleSummary> =
        roleRepository.findAllActive().map { RoleSummary(id = it.id, code = it.code, name = it.name) }
}