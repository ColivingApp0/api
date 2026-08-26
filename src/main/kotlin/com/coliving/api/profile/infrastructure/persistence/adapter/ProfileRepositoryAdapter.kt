package com.coliving.api.profile.infrastructure.persistence.adapter

import com.coliving.api.profile.domain.model.Profile
import com.coliving.api.profile.domain.repository.ProfileRepository
import com.coliving.api.profile.infrastructure.persistence.mapper.ProfileMapper
import com.coliving.api.profile.infrastructure.persistence.repository.ProfileJpaRepository
import java.util.UUID
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class ProfileRepositoryAdapter(
    private val jpaRepository: ProfileJpaRepository,
) : ProfileRepository {

    @Transactional(readOnly = true)
    override fun findByUserId(userId: UUID): Profile? =
        jpaRepository.findByUserId(userId)?.let(ProfileMapper::toDomain)

    @Transactional
    override fun save(profile: Profile) {
        jpaRepository.findByUserId(profile.userId)
            ?.let { existing -> ProfileMapper.copyInto(existing, profile) }
            ?: jpaRepository.save(ProfileMapper.toEntity(profile))
    }
}