package com.coliving.api.profile.application

import com.coliving.api.profile.application.dto.ConfigurePrivacyCommand
import com.coliving.api.profile.application.usecase.ConfigurePrivacyService
import com.coliving.api.profile.domain.enums.GuestType
import com.coliving.api.profile.domain.model.Profile
import com.coliving.api.profile.domain.vo.AcademicInfo
import com.coliving.api.profile.domain.vo.StayPreferences
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigurePrivacyServiceTest {

    private val repository = FakeProfileRepository()
    private val gateway = FakeIdentityGateway()
    private val service = ConfigurePrivacyService(repository, gateway)
    private val userId = UUID.randomUUID()

    @Test
    fun `configures visibility on an empty profile (materializes it)`() {
        val view = service.configure(
            ConfigurePrivacyCommand(
                userId = userId,
                affinityVisible = true,
                academicVisible = true,
            ),
        )

        assertTrue(view.affinityVisible)
        assertTrue(view.academicVisible)
        assertTrue(view.academicVisible && repository.findByUserId(userId)!!.academicInfo.visible)
    }

    @Test
    fun `does not touch profile data when only privacy changes`() {
        repository.save(
            Profile.createEmpty(userId).apply {
                update(
                    "Ana",
                    "3001234567",
                    GuestType.ESTUDIANTE,
                    null,
                    AcademicInfo.empty(),
                    StayPreferences.empty(),
                )
            },
        )

        val view = service.configure(
            ConfigurePrivacyCommand(
                userId = userId,
                affinityVisible = false,
                academicVisible = false,
            ),
        )

        assertEquals("Ana", view.name)
        assertEquals(GuestType.ESTUDIANTE, view.guestType)
        assertFalse(view.affinityVisible)
    }
}