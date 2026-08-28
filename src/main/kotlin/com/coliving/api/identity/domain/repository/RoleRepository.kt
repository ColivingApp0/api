package com.coliving.api.identity.domain.repository

import com.coliving.api.identity.domain.enums.RoleCode
import com.coliving.api.identity.domain.model.Role
import java.util.UUID

/**
 * Out port for reading the role catalog (seeded by Liquibase, admin-managed).
 */
interface RoleRepository {
    fun findById(id: UUID): Role?
    fun findByCode(code: RoleCode): Role?
    fun findAllActive(): List<Role>
}