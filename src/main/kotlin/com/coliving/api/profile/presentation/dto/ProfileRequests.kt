package com.coliving.api.profile.presentation.dto

import com.coliving.api.profile.domain.enums.GuestType
import jakarta.validation.constraints.NotNull
import java.util.UUID

/**
 * RF-010 / RF-011: partial profile update. Every field is optional so PATCH
 * semantics apply; omitted values keep their current data.
 */
data class UpdateProfileRequest(
    val name: String? = null,
    val phone: String? = null,
    val guestType: GuestType? = null,
    val cityId: UUID? = null,
    val institutionId: UUID? = null,
    val facultyId: UUID? = null,
    val careerId: UUID? = null,
    val academicUnlisted: Boolean = false,
    val stayPreferences: List<String> = emptyList(),
)

/** RF-013: visibility switches for affinity and academic data. */
data class ConfigurePrivacyRequest(
    @field:NotNull(message = "affinityVisible is required")
    val affinityVisible: Boolean? = null,
    val academicVisible: Boolean = false,
)