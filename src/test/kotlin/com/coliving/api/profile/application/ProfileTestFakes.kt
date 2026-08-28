package com.coliving.api.profile.application

import com.coliving.api.identity.domain.enums.VerificationLevel
import com.coliving.api.profile.application.port.out.IdentityGateway
import com.coliving.api.profile.domain.model.Profile
import com.coliving.api.profile.domain.repository.ProfileRepository
import java.util.UUID

/** In-memory [ProfileRepository] for profile use-case tests. */
class FakeProfileRepository : ProfileRepository {
    private val store = mutableMapOf<UUID, Profile>()

    override fun findByUserId(userId: UUID): Profile? = store[userId]

    override fun save(profile: Profile) {
        store[profile.userId] = profile
    }
}

/** Stub [IdentityGateway] returning a configurable derived level. */
class FakeIdentityGateway : IdentityGateway {
    var level: VerificationLevel = VerificationLevel.NO_VERIFICADO

    override fun verificationLevelOf(userId: UUID): VerificationLevel = level
}