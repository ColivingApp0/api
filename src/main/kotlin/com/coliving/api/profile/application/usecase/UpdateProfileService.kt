package com.coliving.api.profile.application.usecase

import com.coliving.api.profile.application.dto.ProfileView
import com.coliving.api.profile.application.dto.UpdateProfileCommand
import com.coliving.api.profile.application.dto.toProfileView
import com.coliving.api.profile.application.port.out.IdentityGateway
import com.coliving.api.profile.domain.model.Profile
import com.coliving.api.profile.domain.repository.ProfileRepository
import com.coliving.api.profile.domain.vo.AcademicInfo
import com.coliving.api.profile.domain.vo.StayPreferences
import org.springframework.stereotype.Service

/**
 * RF-010 / RF-011: updates the caller's profile (personal, academic references
 * and stay preferences). The profile row is created on first update.
 * Academic visibility is preserved: only the privacy endpoint changes it.
 */
@Service
class UpdateProfileService(
    private val profileRepository: ProfileRepository,
    private val identityGateway: IdentityGateway,
) {

    fun update(command: UpdateProfileCommand): ProfileView {
        val profile = profileRepository.findByUserId(command.userId) ?: Profile.createEmpty(command.userId)
        profile.update(
            name = command.name,
            phone = command.phone,
            guestType = command.guestType,
            cityId = command.cityId,
            academicInfo = AcademicInfo.of(
                institutionId = command.institutionId,
                facultyId = command.facultyId,
                careerId = command.careerId,
                unlisted = command.academicUnlisted,
                visible = profile.academicInfo.visible,
            ),
            stayPreferences = StayPreferences.of(command.stayPreferenceCodes),
        )
        profileRepository.save(profile)
        return profile.toProfileView(identityGateway.verificationLevelOf(command.userId))
    }
}