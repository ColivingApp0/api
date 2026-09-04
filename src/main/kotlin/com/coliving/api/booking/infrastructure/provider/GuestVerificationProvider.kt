package com.coliving.api.booking.infrastructure.provider

import com.coliving.api.booking.application.port.out.GuestVerificationPort
import com.coliving.api.identity.application.query.UserVerificationQuery
import com.coliving.api.identity.domain.enums.VerificationLevel
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Guest gate (RF-040): a request is only admitted when the guest's
 * identity-derived verification level is COMPLETO — the symmetric requirement
 * to accommodation's host publish gate.
 */
@Component
class GuestVerificationProvider(
    private val userVerificationQuery: UserVerificationQuery,
) : GuestVerificationPort {

    override fun hasMinimumVerification(guestUserId: UUID): Boolean =
        userVerificationQuery.levelOf(guestUserId) == VerificationLevel.COMPLETO
}
