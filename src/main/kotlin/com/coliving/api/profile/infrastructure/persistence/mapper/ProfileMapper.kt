package com.coliving.api.profile.infrastructure.persistence.mapper

import com.coliving.api.profile.domain.model.Profile
import com.coliving.api.profile.domain.vo.AcademicInfo
import com.coliving.api.profile.domain.vo.StayPreferences
import com.coliving.api.profile.infrastructure.persistence.entity.ProfileEntity
import java.time.Instant

object ProfileMapper {

    private const val SEPARATOR = ","

    fun toEntity(profile: Profile): ProfileEntity =
        ProfileEntity(
            userId = profile.userId,
            fullName = profile.name,
            phone = profile.phone,
            guestType = profile.guestType,
            homeCityId = profile.cityId,
            institutionId = profile.academicInfo.institutionId,
            facultyId = profile.academicInfo.facultyId,
            careerId = profile.academicInfo.careerId,
            academicUnlisted = profile.academicInfo.unlisted,
            academicVisible = profile.academicInfo.visible,
            stayPreferences = profile.stayPreferences.codes().joinToString(SEPARATOR),
            affinityVisible = profile.affinityVisible,
            updatedAt = Instant.now(),
        )

    /** Copies the aggregate state onto an existing row (upsert, keeps PK). */
    fun copyInto(entity: ProfileEntity, profile: Profile): ProfileEntity {
        entity.fullName = profile.name
        entity.phone = profile.phone
        entity.guestType = profile.guestType
        entity.homeCityId = profile.cityId
        entity.institutionId = profile.academicInfo.institutionId
        entity.facultyId = profile.academicInfo.facultyId
        entity.careerId = profile.academicInfo.careerId
        entity.academicUnlisted = profile.academicInfo.unlisted
        entity.academicVisible = profile.academicInfo.visible
        entity.stayPreferences = profile.stayPreferences.codes().joinToString(SEPARATOR)
        entity.affinityVisible = profile.affinityVisible
        entity.updatedAt = Instant.now()
        return entity
    }

    fun toDomain(entity: ProfileEntity): Profile =
        Profile(
            userId = entity.userId,
            name = entity.fullName,
            phone = entity.phone,
            guestType = entity.guestType,
            cityId = entity.homeCityId,
            academicInfo = AcademicInfo.of(
                institutionId = entity.institutionId,
                facultyId = entity.facultyId,
                careerId = entity.careerId,
                unlisted = entity.academicUnlisted,
                visible = entity.academicVisible,
            ),
            stayPreferences = StayPreferences.of(
                entity.stayPreferences?.split(SEPARATOR).orEmpty(),
            ),
            affinityVisible = entity.affinityVisible,
        )
}