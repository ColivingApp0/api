package com.coliving.api.identity.application.query

import com.coliving.api.identity.domain.enums.VerificationLevel
import java.util.UUID

/**
 * Query contract that other bounded contexts use to read a user's verification
 * level. The level is derived from the user's verification documents (it is
 * never stored inside `profile`), so `identity` owns both the source and this
 * projection.
 */
interface UserVerificationQuery {
    fun levelOf(userId: UUID): VerificationLevel
}