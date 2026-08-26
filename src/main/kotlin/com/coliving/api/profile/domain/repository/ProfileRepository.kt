package com.coliving.api.profile.domain.repository

import com.coliving.api.profile.domain.model.Profile
import java.util.UUID

/**
 * Persistence port of the `profile` bounded context.
 */
interface ProfileRepository {

    fun findByUserId(userId: UUID): Profile?

    fun save(profile: Profile)
}