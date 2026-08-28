package com.coliving.api.profile.infrastructure.provider

import com.coliving.api.identity.application.query.UserVerificationQuery
import com.coliving.api.identity.domain.enums.VerificationLevel
import com.coliving.api.profile.application.port.out.IdentityGateway
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Adapter from the profile application contract to the identity query side.
 * The verification level is owned (derived) by `identity` and read here.
 */
@Component
class IdentityGatewayAdapter(
    private val userVerificationQuery: UserVerificationQuery,
) : IdentityGateway {

    override fun verificationLevelOf(userId: UUID): VerificationLevel =
        userVerificationQuery.levelOf(userId)
}