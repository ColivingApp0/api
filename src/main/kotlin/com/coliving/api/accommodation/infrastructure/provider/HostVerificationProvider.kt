package com.coliving.api.accommodation.infrastructure.provider

import com.coliving.api.accommodation.application.port.out.HostVerificationPort
import com.coliving.api.identity.application.query.UserVerificationQuery
import com.coliving.api.identity.domain.enums.VerificationLevel
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Adapter from the accommodation port to the identity query side: a host may
 * publish only when identity-derived verification is COMPLETO.
 */
@Component
class HostVerificationProvider(
    private val userVerificationQuery: UserVerificationQuery,
) : HostVerificationPort {

    override fun isVerifiedHost(userId: UUID): Boolean =
        userVerificationQuery.levelOf(userId) == VerificationLevel.COMPLETO
}