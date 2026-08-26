package com.coliving.api.profile.application

import com.coliving.api.profile.application.dto.UpdateProfileCommand
import com.coliving.api.profile.application.usecase.UpdateProfileService
import com.coliving.api.profile.domain.enums.GuestType
import com.coliving.api.profile.domain.model.Profile
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateProfileServiceTest {

    private val repository = FakeProfileRepository()
    private val gateway = FakeIdentityGateway()
    private val service = UpdateProfileService(repository, gateway)
    private val userId = UUID.randomUUID()
    private val institutionId = UUID.randomUUID()
    private val facultyId = UUID.randomUUID()
    private val careerId = UUID.randomUUID()

    @Test
    fun `creates the profile on first update`() {
        val view = service.update(
            UpdateProfileCommand(
                userId = userId,
                name = "Ana",
                phone = "3001234567",
                guestType = GuestType.ESTUDIANTE,
                cityId = null,
                institutionId = institutionId,
                facultyId = facultyId,
                careerId = careerId,
                academicUnlisted = true,
                stayPreferenceCodes = listOf("wifi", "pet"),
            ),
        )

        assertEquals("Ana", view.name)
        assertEquals(GuestType.ESTUDIANTE, view.guestType)
        assertEquals(institutionId, view.institutionId)
        assertEquals(facultyId, view.facultyId)
        assertEquals(careerId, view.careerId)
        assertTrue(view.academicUnlisted)
        assertEquals(listOf("wifi", "pet"), view.stayPreferenceCodes)
        // The stored aggregate is the same object returned to callers.
        assertEquals("Ana", repository.findByUserId(userId)?.name)
    }

    @Test
    fun `updates an existing profile and preserves academic visibility`() {
        repository.save(
            Profile.createEmpty(userId).apply {
                configurePrivacy(affinityVisible = true, academicVisible = true)
            },
        )

        val view = service.update(
            UpdateProfileCommand(
                userId = userId,
                name = "Ana",
                phone = null,
                guestType = null,
                cityId = null,
                institutionId = institutionId,
                facultyId = null,
                careerId = null,
                academicUnlisted = false,
                stayPreferenceCodes = emptyList(),
            ),
        )

        assertTrue(view.affinityVisible)
        assertTrue(view.academicVisible)
        assertEquals(institutionId, view.institutionId)
        assertNull(view.phone)
    }
}