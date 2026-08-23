package com.coliving.api.identity.domain.model

import com.coliving.api.identity.domain.enums.RoleCode
import java.util.UUID

/**
 * Catalog entity for roles (diagram's `Rol`). Seeded by Liquibase with fixed
 * identifiers. `code` maps to [RoleCode] and is what authorization consumes.
 */
class Role private constructor(
    val id: UUID,
    val code: RoleCode,
    val name: String,
    active: Boolean,
) {
    var active: Boolean = active
        private set

    fun activate() {
        active = true
    }

    fun deactivate() {
        active = false
    }

    companion object {
        fun of(id: UUID, code: RoleCode, name: String, active: Boolean = true): Role =
            Role(id, code, name, active)
    }
}