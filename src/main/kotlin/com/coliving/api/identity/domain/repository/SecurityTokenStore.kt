package com.coliving.api.identity.domain.repository

import com.coliving.api.identity.domain.enums.SecurityTokenPurpose
import com.coliving.api.identity.domain.model.SecurityToken
import java.time.Instant
import java.util.UUID

/**
 * Out port for one-time security tokens (RF-001 / RF-003): single use, hash-only storage.
 */
interface SecurityTokenStore {
    fun save(token: SecurityToken)
    fun findByHashAndPurpose(hash: String, purpose: SecurityTokenPurpose): SecurityToken?
    fun markUsed(id: UUID, now: Instant)
}