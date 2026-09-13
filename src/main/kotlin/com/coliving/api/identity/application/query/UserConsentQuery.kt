package com.coliving.api.identity.application.query

import com.coliving.api.identity.domain.enums.ConsentType
import java.util.UUID

/**
 * Query contract that other bounded contexts use to read whether a user
 * granted a consent. Mirrors [UserVerificationQuery]: identity owns the consent
 * records and this projection, so consumers never read identity tables.
 *
 * `community` uses it for RF-062 (aggregated community information is only
 * exposed when there is sufficient consent).
 */
interface UserConsentQuery {

    /** Whether [userId] currently has an accepted consent of [type]. */
    fun hasAccepted(userId: UUID, type: ConsentType): Boolean
}