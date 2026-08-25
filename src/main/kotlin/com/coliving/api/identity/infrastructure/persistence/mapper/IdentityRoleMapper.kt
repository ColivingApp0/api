package com.coliving.api.identity.infrastructure.persistence.mapper

import com.coliving.api.identity.domain.model.Role
import com.coliving.api.identity.infrastructure.persistence.entity.IdentityRoleEntity

object IdentityRoleMapper {

    fun toDomain(entity: IdentityRoleEntity): Role =
        Role.of(
            id = entity.id,
            code = entity.code,
            name = entity.name,
            active = entity.active,
        )
}