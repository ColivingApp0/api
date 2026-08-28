package com.coliving.api.profile.infrastructure.persistence.entity

import com.coliving.api.profile.domain.enums.GuestType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * JPA entity for `profile_profile`. The user id is the identity anchor: the
 * FK to `identity_user` keeps profiles alive only for existing users (same
 * pattern as `identity_consent`).
 */
@Entity
@Table(name = "profile_profile")
class ProfileEntity(
    @Id
    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "full_name", length = 255)
    var fullName: String? = null,

    @Column(name = "phone", length = 50)
    var phone: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "guest_type", length = 50)
    var guestType: GuestType? = null,

    @Column(name = "home_city_id")
    var homeCityId: UUID? = null,

    @Column(name = "institution_id")
    var institutionId: UUID? = null,

    @Column(name = "faculty_id")
    var facultyId: UUID? = null,

    @Column(name = "career_id")
    var careerId: UUID? = null,

    @Column(name = "academic_unlisted", nullable = false)
    var academicUnlisted: Boolean = false,

    @Column(name = "academic_visible", nullable = false)
    var academicVisible: Boolean = false,

    @Column(name = "stay_preferences", length = 500)
    var stayPreferences: String? = null,

    @Column(name = "affinity_visible", nullable = false)
    var affinityVisible: Boolean = false,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}