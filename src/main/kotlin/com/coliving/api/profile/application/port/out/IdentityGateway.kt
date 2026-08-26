package com.coliving.api.profile.application.port.out

import com.coliving.api.identity.domain.enums.VerificationLevel
import java.util.UUID

/**
 * Out port used by profile use cases to read identity-derived state. Currently
 * exposes the verification level, which is owned (derived) by `identity` and
 * must never be stored inside the profile again.
 */
interface IdentityGateway {

    fun verificationLevelOf(userId: UUID): VerificationLevel
}