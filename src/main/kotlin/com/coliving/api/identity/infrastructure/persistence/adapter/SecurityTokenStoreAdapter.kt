package com.coliving.api.identity.infrastructure.persistence.adapter

import com.coliving.api.identity.domain.enums.SecurityTokenPurpose
import com.coliving.api.identity.domain.model.SecurityToken
import com.coliving.api.identity.domain.repository.SecurityTokenStore
import com.coliving.api.identity.infrastructure.persistence.mapper.IdentitySecurityTokenMapper
import com.coliving.api.identity.infrastructure.persistence.repository.IdentitySecurityTokenJpaRepository
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class SecurityTokenStoreAdapter(
    private val securityTokenJpaRepository: IdentitySecurityTokenJpaRepository,
) : SecurityTokenStore {

    @Transactional
    override fun save(token: SecurityToken) {
        securityTokenJpaRepository.save(IdentitySecurityTokenMapper.toEntity(token))
    }

    override fun findByHashAndPurpose(hash: String, purpose: SecurityTokenPurpose): SecurityToken? =
        securityTokenJpaRepository.findByTokenHashAndPurpose(hash, purpose)
            ?.let(IdentitySecurityTokenMapper::toDomain)

    @Transactional
    override fun markUsed(id: UUID, now: Instant) {
        val entity = securityTokenJpaRepository.findById(id).orElse(null) ?: return
        entity.usedAt = now
        securityTokenJpaRepository.save(entity)
    }
}