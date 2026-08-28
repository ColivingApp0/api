package com.coliving.api.identity.infrastructure.persistence.mapper

import com.coliving.api.identity.domain.model.UserRole
import com.coliving.api.identity.infrastructure.persistence.entity.IdentityUserRoleEntity

object IdentityUserRoleMapper {

    fun toDomain(entity: IdentityUserRoleEntity): UserRole =
        UserRole.restore(
            id = entity.id,
            userId = entity.userId,
            roleId = entity.roleId,
            roleCode = entity.roleCode,
            isActive = entity.isActive,
            assignedAt = entity.assignedAt,
        )

    fun toEntity(userRole: UserRole): IdentityUserRoleEntity = IdentityUserRoleEntity(
        id = userRole.id,
        userId = userRole.userId,
        roleId = userRole.roleId,
        roleCode = userRole.roleCode,
        isActive = userRole.isActive,
        assignedAt = userRole.assignedAt,
    )
}