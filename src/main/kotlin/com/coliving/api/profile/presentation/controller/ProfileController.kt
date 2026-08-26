package com.coliving.api.profile.presentation.controller

import com.coliving.api.profile.application.dto.ConfigurePrivacyCommand
import com.coliving.api.profile.application.dto.ProfileView
import com.coliving.api.profile.application.dto.UpdateProfileCommand
import com.coliving.api.profile.application.usecase.ConfigurePrivacyService
import com.coliving.api.profile.application.usecase.GetProfileService
import com.coliving.api.profile.application.usecase.UpdateProfileService
import com.coliving.api.profile.presentation.dto.ConfigurePrivacyRequest
import com.coliving.api.profile.presentation.dto.UpdateProfileRequest
import com.coliving.api.shared.security.CurrentUser
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Authenticated profile endpoints (RF-010, RF-011, RF-013, RF-014). The
 * principal always is the shared [CurrentUser]; the owner is the only subject.
 */
@RestController
@RequestMapping("/api/v1/profile")
class ProfileController(
    private val getProfileService: GetProfileService,
    private val updateProfileService: UpdateProfileService,
    private val configurePrivacyService: ConfigurePrivacyService,
) {

    @GetMapping
    fun get(@AuthenticationPrincipal current: CurrentUser): ProfileView =
        getProfileService.get(current.userId)

    @PatchMapping
    fun update(
        @AuthenticationPrincipal current: CurrentUser,
        @Valid @RequestBody request: UpdateProfileRequest,
    ): ProfileView = updateProfileService.update(
        UpdateProfileCommand(
            userId = current.userId,
            name = request.name,
            phone = request.phone,
            guestType = request.guestType,
            cityId = request.cityId,
            institutionId = request.institutionId,
            facultyId = request.facultyId,
            careerId = request.careerId,
            academicUnlisted = request.academicUnlisted,
            stayPreferenceCodes = request.stayPreferences,
        ),
    )

    @PutMapping("/privacy")
    fun configurePrivacy(
        @AuthenticationPrincipal current: CurrentUser,
        @Valid @RequestBody request: ConfigurePrivacyRequest,
    ): ProfileView = configurePrivacyService.configure(
        ConfigurePrivacyCommand(
            userId = current.userId,
            affinityVisible = request.affinityVisible ?: false,
            academicVisible = request.academicVisible,
        ),
    )
}