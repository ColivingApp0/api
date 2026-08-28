package com.coliving.api.profile.infrastructure.provider

import com.coliving.api.identity.application.port.out.ProfileCompletenessPort
import com.coliving.api.identity.domain.enums.RoleCode
import com.coliving.api.profile.domain.repository.ProfileRepository
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Implements the `identity` out port using the profile aggregate. Each role is
 * directed to the completeness rule it needs: hosts need name + phone, guests
 * need name + guest type (RF-004, RF-010, RF-013).
 */
@Component
class ProfileCompletenessProvider(
    private val profileRepository: ProfileRepository,
) : ProfileCompletenessPort {

    override fun hasMinimumDataFor(userId: UUID, roleCode: RoleCode): Boolean {
        val profile = profileRepository.findByUserId(userId) ?: return false
        return when (roleCode) {
            RoleCode.ANFITRION -> profile.hasHostBasics()
            RoleCode.HUESPED_ESTUDIANTE, RoleCode.HUESPED_TURISTA -> profile.hasGuestBasics()
            else -> true
        }
    }
}