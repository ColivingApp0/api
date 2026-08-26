package com.coliving.api.profile.application.usecase

import com.coliving.api.profile.application.dto.ProfileView
import com.coliving.api.profile.application.dto.toProfileView
import com.coliving.api.profile.application.port.out.IdentityGateway
import com.coliving.api.profile.domain.model.Profile
import com.coliving.api.profile.domain.repository.ProfileRepository
import java.util.UUID
import org.springframework.stereotype.Service

/**
 * RF-010: reads the caller's profile. An empty profile is returned the first
 * time (the aggregate is created lazily on first update), always enriched with
 * the identity-derived verification level.
 */
@Service
class GetProfileService(
    private val profileRepository: ProfileRepository,
    private val identityGateway: IdentityGateway,
) {

    fun get(userId: UUID): ProfileView {
        val profile = profileRepository.findByUserId(userId) ?: Profile.createEmpty(userId)
        return profile.toProfileView(identityGateway.verificationLevelOf(userId))
    }
}