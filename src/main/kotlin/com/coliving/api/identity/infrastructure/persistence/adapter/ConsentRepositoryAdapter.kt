package com.coliving.api.identity.infrastructure.persistence.adapter

import com.coliving.api.identity.domain.enums.ConsentType
import com.coliving.api.identity.domain.model.Consent
import com.coliving.api.identity.domain.repository.ConsentRepository
import com.coliving.api.identity.infrastructure.persistence.mapper.IdentityConsentMapper
import com.coliving.api.identity.infrastructure.persistence.repository.IdentityConsentJpaRepository
import java.util.UUID
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class ConsentRepositoryAdapter(
    private val consentJpaRepository: IdentityConsentJpaRepository,
) : ConsentRepository {

    override fun findByUserAndType(userId: UUID, type: ConsentType): Consent? =
        consentJpaRepository.findByUserIdAndType(userId, type)?.let(IdentityConsentMapper::toDomain)

    override fun findByUser(userId: UUID): List<Consent> =
        consentJpaRepository.findByUserIdOrderByAcceptedAtDesc(userId)
            .map(IdentityConsentMapper::toDomain)

    @Transactional
    override fun save(consent: Consent) {
        consentJpaRepository.save(IdentityConsentMapper.toEntity(consent))
    }
}