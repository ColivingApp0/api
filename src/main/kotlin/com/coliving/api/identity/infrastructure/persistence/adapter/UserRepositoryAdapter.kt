package com.coliving.api.identity.infrastructure.persistence.adapter

import com.coliving.api.identity.domain.model.User
import com.coliving.api.identity.domain.model.UserRole
import com.coliving.api.identity.domain.repository.UserRepository
import com.coliving.api.identity.domain.vo.Email
import com.coliving.api.identity.infrastructure.persistence.mapper.IdentityUserMapper
import com.coliving.api.identity.infrastructure.persistence.mapper.IdentityUserRoleMapper
import com.coliving.api.identity.infrastructure.persistence.repository.IdentityUserJpaRepository
import com.coliving.api.identity.infrastructure.persistence.repository.IdentityUserRoleJpaRepository
import java.util.UUID
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class UserRepositoryAdapter(
    private val userJpaRepository: IdentityUserJpaRepository,
    private val userRoleJpaRepository: IdentityUserRoleJpaRepository,
) : UserRepository {

    override fun findById(id: UUID): User? =
        userJpaRepository.findById(id).orElse(null)?.let { load(it) }

    override fun findByEmail(email: Email): User? =
        userJpaRepository.findByEmail(email.value)?.let { load(it) }

    override fun existsByEmail(email: Email): Boolean =
        userJpaRepository.existsByEmail(email.value)

    @Transactional
    override fun save(user: User) {
        userJpaRepository.save(IdentityUserMapper.toEntity(user))

        // Synchronize the role assignments (child records of the aggregate).
        val existing = userRoleJpaRepository.findByUserId(user.id)
        val desired = user.roles.map { IdentityUserRoleMapper.toEntity(it) }
        userRoleJpaRepository.deleteAll(existing)
        userRoleJpaRepository.saveAll(desired)
    }

    /** Loads the aggregate root including its [UserRole] assignments. */
    private fun load(entity: com.coliving.api.identity.infrastructure.persistence.entity.IdentityUserEntity): User {
        val roles = userRoleJpaRepository.findByUserId(entity.id)
            .map(IdentityUserRoleMapper::toDomain)
            .toSet()
        return IdentityUserMapper.toDomain(
            id = entity.id,
            email = entity.email,
            passwordHash = entity.passwordHash,
            status = entity.status,
            emailVerified = entity.emailVerified,
            createdAt = entity.createdAt,
            roles = roles,
        )
    }

}