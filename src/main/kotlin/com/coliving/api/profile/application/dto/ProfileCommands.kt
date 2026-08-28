package com.coliving.api.profile.application.dto

import com.coliving.api.profile.domain.enums.GuestType
import java.util.UUID

/** RF-010 / RF-011: partial profile update. */
data class UpdateProfileCommand(
    val userId: UUID,
    val name: String?,
    val phone: String?,
    val guestType: GuestType?,
    val cityId: UUID?,
    val institutionId: UUID?,
    val facultyId: UUID?,
    val careerId: UUID?,
    val academicUnlisted: Boolean = false,
    val stayPreferenceCodes: List<String> = emptyList(),
)

/** RF-013: visibility switches for affinity and academic data. */
data class ConfigurePrivacyCommand(
    val userId: UUID,
    val affinityVisible: Boolean,
    val academicVisible: Boolean = false,
)