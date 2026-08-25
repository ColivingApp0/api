package com.coliving.api.identity.domain.model

import com.coliving.api.identity.domain.enums.SecurityTokenPurpose
import com.coliving.api.shared.error.ConflictException
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * One-time security token used for email verification (RF-001) and password
 * reset (RF-003). Only the hash is persisted; [usedAt] enforces single use
 * and [expiresAt] bounds validity, matching the SRS acceptance criteria
 * ("el enlace o código expira, solo puede usarse una vez").
 */
class SecurityToken(
    val id: UUID,
    val userId: UUID,
    val purpose: SecurityTokenPurpose,
    val tokenHash: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    usedAt: Instant?,
) {
    var usedAt: Instant? = usedAt
        private set

    fun isExpired(now: Instant): Boolean = !now.isBefore(expiresAt)

    /** Consumes the token: fails if already used or expired. */
    fun consume(now: Instant) {
        if (usedAt != null) throw ConflictException("Token already used")
        if (isExpired(now)) throw ConflictException("Token expired")
        usedAt = now
    }

    companion object {
        fun create(
            userId: UUID,
            purpose: SecurityTokenPurpose,
            tokenHash: String,
            now: Instant,
            ttl: Duration,
        ): SecurityToken = SecurityToken(
            id = UUID.randomUUID(),
            userId = userId,
            purpose = purpose,
            tokenHash = tokenHash,
            createdAt = now,
            expiresAt = now.plus(ttl),
            usedAt = null,
        )
    }
}