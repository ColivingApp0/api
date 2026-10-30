package com.coliving.api.profile.application.dto

import com.coliving.api.identity.domain.enums.VerificationLevel
import com.coliving.api.profile.domain.enums.GuestType
import com.coliving.api.profile.domain.model.Profile
import java.util.UUID

/**
 * Profile view. [verificationLevel] is a projection obtained from `identity`.
 * [roommateProfileVisible] is the explicit consent required to reveal the
 * profile to potential roommates.
 */
data class ProfileView(
    val userId: UUID,
    val name: String?,
    val phone: String?,
    val guestType: GuestType?,
    val cityId: UUID?,
    val institutionId: UUID?,
    val facultyId: UUID?,
    val careerId: UUID?,
    val academicUnlisted: Boolean,
    val academicVisible: Boolean,
    val stayPreferenceCodes: List<String>,
    val affinityVisible: Boolean,
    val avatarUrl: String?,
    val roommateProfileVisible: Boolean,
    val verificationLevel: VerificationLevel,
)

internal fun Profile.toProfileView(verificationLevel: VerificationLevel): ProfileView =
    ProfileView(
        userId = userId,
        name = name,
        phone = phone,
        guestType = guestType,
        cityId = cityId,
        institutionId = academicInfo.institutionId,
        facultyId = academicInfo.facultyId,
        careerId = academicInfo.careerId,
        academicUnlisted = academicInfo.unlisted,
        academicVisible = academicInfo.visible,
        stayPreferenceCodes = stayPreferences.codes(),
        affinityVisible = affinityVisible,
        avatarUrl = avatarUrl,
        roommateProfileVisible = roommateProfileVisible,
        verificationLevel = verificationLevel,
    )

/** Minimal public card; no phone, documents, academic data or contact details. */
data class PublicProfileCard(
    val userId: UUID,
    val name: String?,
    val avatarUrl: String?,
    val roommateProfileVisible: Boolean,
)
