package com.coliving.api.reputation.infrastructure.provider

import com.coliving.api.identity.application.query.UserVerificationQuery
import com.coliving.api.reputation.application.port.out.UserVerificationPort
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Adapter over identity's [UserVerificationQuery]: the verification level is a
 * documented factor of the score (RF-071), so a verified user is not scored the
 * same as an unverified one.
 */
@Component
class IdentityVerificationProvider(
    private val userVerificationQuery: UserVerificationQuery,
) : UserVerificationPort {

    override fun levelOf(userId: UUID): String = userVerificationQuery.levelOf(userId).name
}