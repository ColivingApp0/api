package com.coliving.api.identity.domain.model

import com.coliving.api.identity.domain.enums.RoleCode
import java.time.Instant
import java.util.UUID

/**
 * Maps the diagram's `UsuarioRol` bridge entity (a table with attributes of its
 * own). It is the single source of truth for the user's active role (RF-004).
 * Note: the role reference is kept through [roleId], not an object reference.
 */
class UserRole private constructor(
    val id: UUID,
    val userId: UUID,
    val roleId: UUID,
    val roleCode: RoleCode,
    isActive: Boolean,
    val assignedAt: Instant,
) {
    var isActive: Boolean = isActive
        private set

    fun activate() {
        isActive = true
    }

    fun deactivate() {
        isActive = false
    }

    companion object {
        fun create(userId: UUID, role: Role, assignedAt: Instant): UserRole =
            UserRole(
                id = UUID.randomUUID(),
                userId = userId,
                roleId = role.id,
                roleCode = role.code,
                isActive = true,
                assignedAt = assignedAt,
            )

        fun restore(
            id: UUID,
            userId: UUID,
            roleId: UUID,
            roleCode: RoleCode,
            isActive: Boolean,
            assignedAt: Instant,
        ): UserRole = UserRole(id, userId, roleId, roleCode, isActive, assignedAt)
    }
}