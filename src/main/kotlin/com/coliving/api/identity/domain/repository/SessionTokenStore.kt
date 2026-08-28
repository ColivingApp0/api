package com.coliving.api.identity.domain.repository

import com.coliving.api.identity.domain.model.SessionToken
import java.time.Instant
import java.util.UUID

/**
 * Out port for opaque session tokens (RF-002).
 *
 * Sessions are addressed by their hash: the raw value is only known to the
 * client. [revokeByHash] implements individual logout for one device while
 * [revokeAllForUser] supports revoking every session of a user.
 */
interface SessionTokenStore {
    fun save(token: SessionToken)
    fun findByHash(hash: String): SessionToken?
    fun revokeByHash(hash: String, now: Instant): Boolean
    fun revokeAllForUser(userId: UUID, now: Instant)
}