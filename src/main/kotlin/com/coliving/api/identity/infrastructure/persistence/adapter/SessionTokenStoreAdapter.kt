package com.coliving.api.identity.infrastructure.persistence.adapter

import com.coliving.api.identity.domain.model.SessionToken
import com.coliving.api.identity.domain.repository.SessionTokenStore
import com.coliving.api.identity.infrastructure.persistence.mapper.IdentitySessionMapper
import com.coliving.api.identity.infrastructure.persistence.repository.IdentitySessionJpaRepository
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class SessionTokenStoreAdapter(
    private val sessionJpaRepository: IdentitySessionJpaRepository,
) : SessionTokenStore {

    @Transactional
    override fun save(token: SessionToken) {
        sessionJpaRepository.save(IdentitySessionMapper.toEntity(token))
    }

    override fun findByHash(hash: String): SessionToken? =
        sessionJpaRepository.findByTokenHash(hash)?.let(IdentitySessionMapper::toDomain)

    @Transactional
    override fun revokeByHash(hash: String, now: Instant): Boolean {
        val entity = sessionJpaRepository.findByTokenHash(hash) ?: return false
        if (entity.revokedAt == null) entity.revokedAt = now
        sessionJpaRepository.save(entity)
        return true
    }

    @Transactional
    override fun revokeAllForUser(userId: UUID, now: Instant) {
        val sessions = sessionJpaRepository.findByUserId(userId)
        sessions.forEach { if (it.revokedAt == null) it.revokedAt = now }
        sessionJpaRepository.saveAll(sessions)
    }
}