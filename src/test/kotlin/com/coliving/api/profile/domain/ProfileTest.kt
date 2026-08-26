package com.coliving.api.profile.domain

import com.coliving.api.profile.domain.enums.GuestType
import com.coliving.api.profile.domain.model.Profile
import com.coliving.api.profile.domain.vo.AcademicInfo
import com.coliving.api.profile.domain.vo.StayPreferences
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileTest {

    private val userId = UUID.randomUUID()

    @Test
    fun `createEmpty starts with no data and default privacy`() {
        val profile = Profile.createEmpty(userId)

        assertEquals(userId, profile.userId)
        assertNull(profile.name)
        assertNull(profile.phone)
        assertNull(profile.guestType)
        assertNull(profile.cityId)
        assertTrue(profile.stayPreferences.isEmpty())
        assertTrue(profile.academicInfo.isEmpty())
        assertFalse(profile.affinityVisible)
        assertFalse(profile.academicInfo.visible)
    }

    @Test
    fun `update normalizes blank values and de-duplicates preferences`() {
        val profile = Profile.createEmpty(userId)
        profile.update(
            name = "  Ana  ",
            phone = "   ",
            guestType = GuestType.ESTUDIANTE,
            cityId = UUID.randomUUID(),
            academicInfo = AcademicInfo.of(UUID.randomUUID(), null, null, false, false),
            stayPreferences = StayPreferences.of(listOf("wifi", "pet", "wifi")),
        )

        assertEquals("Ana", profile.name)
        assertNull(profile.phone)
        assertEquals(GuestType.ESTUDIANTE, profile.guestType)
        assertEquals(listOf("wifi", "pet"), profile.stayPreferences.codes())
    }

    @Test
    fun `configurePrivacy controls affinity and academic visibility`() {
        val profile = Profile.createEmpty(userId)

        profile.configurePrivacy(affinityVisible = true, academicVisible = false)
        assertTrue(profile.affinityVisible)
        assertFalse(profile.academicInfo.visible)

        profile.configurePrivacy(affinityVisible = true, academicVisible = true)
        assertTrue(profile.academicInfo.visible)
    }

    @Test
    fun `guest completeness requires name and guest type`() {
        val profile = Profile.createEmpty(userId)
        assertFalse(profile.hasGuestBasics())

        profile.update("Ana", "3001234567", null, null, AcademicInfo.empty(), StayPreferences.empty())
        assertFalse(profile.hasGuestBasics())

        profile.update("Ana", "3001234567", GuestType.TURISTA, null, AcademicInfo.empty(), StayPreferences.empty())
        assertTrue(profile.hasGuestBasics())
    }

    @Test
    fun `host completeness requires name and phone`() {
        val profile = Profile.createEmpty(userId)

        profile.update("Ana", null, null, null, AcademicInfo.empty(), StayPreferences.empty())
        assertFalse(profile.hasHostBasics())

        profile.update("Ana", "3001234567", null, null, AcademicInfo.empty(), StayPreferences.empty())
        assertTrue(profile.hasHostBasics())
    }
}