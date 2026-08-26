package com.coliving.api.profile.presentation

import com.coliving.api.identity.domain.enums.VerificationLevel
import com.coliving.api.identity.infrastructure.security.BearerTokenAuthenticationFilter
import com.coliving.api.profile.application.dto.ConfigurePrivacyCommand
import com.coliving.api.profile.application.dto.ProfileView
import com.coliving.api.profile.application.dto.UpdateProfileCommand
import com.coliving.api.profile.application.usecase.ConfigurePrivacyService
import com.coliving.api.profile.application.usecase.GetProfileService
import com.coliving.api.profile.application.usecase.UpdateProfileService
import com.coliving.api.profile.domain.enums.GuestType
import com.coliving.api.profile.presentation.controller.ProfileController
import com.coliving.api.shared.security.CurrentUser
import java.util.UUID
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    value = [ProfileController::class],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [BearerTokenAuthenticationFilter::class],
        ),
    ],
)
class ProfileControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var getProfileService: GetProfileService

    @MockitoBean
    lateinit var updateProfileService: UpdateProfileService

    @MockitoBean
    lateinit var configurePrivacyService: ConfigurePrivacyService

    private val userId = UUID.randomUUID()
    private val principal = CurrentUser(userId, "guest@example.com", setOf("HUESPED_ESTUDIANTE"))
    private val authentication = UsernamePasswordAuthenticationToken(
        principal,
        null,
        principal.authorities(),
    )

    @Test
    fun `get endpoint returns the current profile`() {
        val view = ProfileView(
            userId = userId,
            name = "Ana",
            phone = "3001234567",
            guestType = GuestType.ESTUDIANTE,
            cityId = null,
            institutionId = null,
            facultyId = null,
            careerId = null,
            academicUnlisted = false,
            academicVisible = false,
            stayPreferenceCodes = listOf("wifi"),
            affinityVisible = false,
            verificationLevel = VerificationLevel.NO_VERIFICADO,
        )
        `when`(getProfileService.get(userId)).thenReturn(view)

        mockMvc.perform(get("/api/v1/profile").with(authentication(authentication)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.name").value("Ana"))
            .andExpect(jsonPath("$.guestType").value("ESTUDIANTE"))
            .andExpect(jsonPath("$.verificationLevel").value("NO_VERIFICADO"))
    }

    @Test
    fun `patch endpoint maps fields and returns the updated profile`() {
        val view = ProfileView(
            userId = userId,
            name = "Ana",
            phone = null,
            guestType = GuestType.ESTUDIANTE,
            cityId = null,
            institutionId = null,
            facultyId = null,
            careerId = null,
            academicUnlisted = false,
            academicVisible = false,
            stayPreferenceCodes = emptyList(),
            affinityVisible = false,
            verificationLevel = VerificationLevel.NO_VERIFICADO,
        )
        `when`(
            updateProfileService.update(
                UpdateProfileCommand(
                    userId = userId,
                    name = "Ana",
                    phone = null,
                    guestType = GuestType.ESTUDIANTE,
                    cityId = null,
                    institutionId = null,
                    facultyId = null,
                    careerId = null,
                    academicUnlisted = false,
                    stayPreferenceCodes = emptyList(),
                ),
            ),
        ).thenReturn(view)

        mockMvc.perform(
            patch("/api/v1/profile")
                .with(authentication(authentication))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Ana","guestType":"ESTUDIANTE"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Ana"))

        verify(updateProfileService).update(
            UpdateProfileCommand(
                userId = userId,
                name = "Ana",
                phone = null,
                guestType = GuestType.ESTUDIANTE,
                cityId = null,
                institutionId = null,
                facultyId = null,
                careerId = null,
                academicUnlisted = false,
                stayPreferenceCodes = emptyList(),
            ),
        )
    }

    @Test
    fun `PATCH with an invalid enum value is rejected`() {
        mockMvc.perform(
            patch("/api/v1/profile")
                .with(authentication(authentication))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"guestType":"DESCONOCIDO"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
    }

    @Test
    fun `privacy endpoint configures visibility`() {
        val view = ProfileView(
            userId = userId,
            name = null,
            phone = null,
            guestType = null,
            cityId = null,
            institutionId = null,
            facultyId = null,
            careerId = null,
            academicUnlisted = false,
            academicVisible = true,
            stayPreferenceCodes = emptyList(),
            affinityVisible = true,
            verificationLevel = VerificationLevel.NO_VERIFICADO,
        )
        `when`(
            configurePrivacyService.configure(
                ConfigurePrivacyCommand(
                    userId = userId,
                    affinityVisible = true,
                    academicVisible = true,
                ),
            ),
        ).thenReturn(view)

        mockMvc.perform(
            put("/api/v1/profile/privacy")
                .with(authentication(authentication))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"affinityVisible":true,"academicVisible":true}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.affinityVisible").value(true))
            .andExpect(jsonPath("$.academicVisible").value(true))
    }
}