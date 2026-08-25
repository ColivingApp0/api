package com.coliving.api.identity.infrastructure.persistence.adapter

import com.coliving.api.identity.domain.enums.RoleCode
import com.coliving.api.identity.domain.model.Role
import com.coliving.api.identity.domain.repository.RoleRepository
import com.coliving.api.identity.infrastructure.persistence.mapper.IdentityRoleMapper
import com.coliving.api.identity.infrastructure.persistence.repository.IdentityRoleJpaRepository
import java.util.UUID
import org.springframework.stereotype.Repository

@Repository
class RoleRepositoryAdapter(
    private val roleJpaRepository: IdentityRoleJpaRepository,
) : RoleRepository {

    override fun findById(id: UUID): Role? =
        roleJpaRepository.findById(id).orElse(null)?.let(IdentityRoleMapper::toDomain)

    override fun findByCode(code: RoleCode): Role? =
        roleJpaRepository.findByCode(code)?.let(IdentityRoleMapper::toDomain)

    override fun findAllActive(): List<Role> =
        roleJpaRepository.findByActiveTrue().map(IdentityRoleMapper::toDomain)
}