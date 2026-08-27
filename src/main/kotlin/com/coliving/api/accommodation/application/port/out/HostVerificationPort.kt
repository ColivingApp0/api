package com.coliving.api.accommodation.application.port.out

import java.util.UUID

/**
 * Out port for the publish gate: the host must be identity-verified
 * (COMPLETO). Implemented by an adapter that talks to `identity`'s
 * UserVerificationQuery — same pattern as the profile context.
 */
interface HostVerificationPort {

    fun isVerifiedHost(userId: UUID): Boolean
}