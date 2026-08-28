package com.coliving.api.profile.application.usecase

import com.coliving.api.profile.application.dto.ConfigurePrivacyCommand
import com.coliving.api.profile.application.dto.ProfileView
import com.coliving.api.profile.application.dto.toProfileView
import com.coliving.api.profile.application.port.out.IdentityGateway
import com.coliving.api.profile.domain.model.Profile
import com.coliving.api.profile.domain.repository.ProfileRepository
import org.springframework.stereotype.Service

/**
 * RF-013: the user decides which affinity and academic data may be shown to
 * other users. Empty profiles are materialized on first configuration.
 */
@Service
class ConfigurePrivacyService(
    private val profileRepository: ProfileRepository,
    private val identityGateway: IdentityGateway,
) {

    fun configure(command: ConfigurePrivacyCommand): ProfileView {
        val profile = profileRepository.findByUserId(command.userId) ?: Profile.createEmpty(command.userId)
        profile.configurePrivacy(command.affinityVisible, command.academicVisible)
        profileRepository.save(profile)
        return profile.toProfileView(identityGateway.verificationLevelOf(command.userId))
    }
}