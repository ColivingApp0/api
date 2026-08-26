package com.coliving.api.profile.domain.model

import com.coliving.api.profile.domain.enums.GuestType
import com.coliving.api.profile.domain.vo.AcademicInfo
import com.coliving.api.profile.domain.vo.StayPreferences
import java.util.UUID

/**
 * Profile aggregate (diagram's `Perfil`), owned by the `profile` bounded
 * context and keyed by the identity user id (FK to `identity_user`, same
 * pattern as `identity_consent`). Holds personal data, guest type, stay
 * preferences, academic references and privacy flags.
 *
 * [cityId] and [AcademicInfo.institutionId/facultyId/careerId] reference
 * catalogs of other bounded contexts through identifiers only — never as JPA
 * relations. The verification level is NOT stored here; it is a projection
 * derived by the `identity` context.
 */
class Profile(
    val userId: UUID,
    name: String?,
    phone: String?,
    guestType: GuestType?,
    cityId: UUID?,
    academicInfo: AcademicInfo,
    stayPreferences: StayPreferences,
    affinityVisible: Boolean,
) {

    var name: String? = name
        private set

    var phone: String? = phone
        private set

    var guestType: GuestType? = guestType
        private set

    var cityId: UUID? = cityId
        private set

    var academicInfo: AcademicInfo = academicInfo
        private set

    var stayPreferences: StayPreferences = stayPreferences
        private set

    var affinityVisible: Boolean = affinityVisible
        private set

    /** RF-010 / RF-011: profile update; blank strings normalize to null. */
    fun update(
        name: String?,
        phone: String?,
        guestType: GuestType?,
        cityId: UUID?,
        academicInfo: AcademicInfo,
        stayPreferences: StayPreferences,
    ) {
        this.name = normalize(name)
        this.phone = normalize(phone)
        this.guestType = guestType
        this.cityId = cityId
        this.academicInfo = academicInfo
        this.stayPreferences = stayPreferences
    }

    /** RF-013: the user controls which affinity/academic data may be shown. */
    fun configurePrivacy(affinityVisible: Boolean, academicVisible: Boolean) {
        this.affinityVisible = affinityVisible
        this.academicInfo = academicInfo.withVisible(academicVisible)
    }

    /** Completeness rule for the ANFITRION role (RF-004 / RF-013). */
    fun hasHostBasics(): Boolean = !name.isNullOrBlank() && !phone.isNullOrBlank()

    /** Completeness rule for the guest roles (RF-004 / RF-010). */
    fun hasGuestBasics(): Boolean = !name.isNullOrBlank() && guestType != null

    private fun normalize(value: String?): String? =
        value?.trim()?.takeIf { it.isNotBlank() }

    companion object {
        fun createEmpty(userId: UUID): Profile = Profile(
            userId = userId,
            name = null,
            phone = null,
            guestType = null,
            cityId = null,
            academicInfo = AcademicInfo.empty(),
            stayPreferences = StayPreferences.empty(),
            affinityVisible = false,
        )
    }
}