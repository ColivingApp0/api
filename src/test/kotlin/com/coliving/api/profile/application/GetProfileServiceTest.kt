package com.coliving.api.profile.application

import com.coliving.api.identity.domain.enums.VerificationLevel
import com.coliving.api.profile.application.usecase.GetProfileService
import com.coliving.api.profile.domain.enums.GuestType
import com.coliving.api.profile.domain.model.Profile
import com.coliving.api.profile.domain.vo.AcademicInfo
import com.coliving.api.profile.domain.vo.StayPreferences
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetProfileServiceTest {

    private val repository = FakeProfileRepository()
    private val gateway = FakeIdentityGateway()
    private val service = GetProfileService(repository, gateway)
    private val userId = UUID.randomUUID()

    @Test
    fun `returns an empty profile enriched with the derived verification level`() {
        gateway.level = VerificationLevel.COMPLETO

        val view = service.get(userId)

        assertNull(view.name)
        assertNull(view.guestType)
        assertEquals(VerificationLevel.COMPLETO, view.verificationLevel)
    }

    @Test
    fun `returns the stored profile with the identity-derived level`() {
        gateway.level = VerificationLevel.NO_VERIFICADO
        repository.save(
            Profile.createEmpty(userId).apply {
                update(
                    "Ana",
                    "3001234567",
                    GuestType.ESTUDIANTE,
                    null,
                    AcademicInfo.empty(),
                    StayPreferences.of(listOf("wifi")),
                )
            },
        )

        val view = service.get(userId)

        assertEquals("Ana", view.name)
        assertEquals(GuestType.ESTUDIANTE, view.guestType)
        assertEquals(listOf("wifi"), view.stayPreferenceCodes)
        assertEquals(VerificationLevel.NO_VERIFICADO, view.verificationLevel)
    }
}