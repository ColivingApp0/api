package com.coliving.api.identity.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "identity_session",
    indexes = [
        Index(name = "uk_identity_session_hash", columnList = "token_hash", unique = true),
        Index(name = "idx_identity_session_user", columnList = "user_id"),
    ],
)
class IdentitySessionEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    /** SHA-256 hex of the raw opaque token; the raw value is never persisted. */
    @Column(name = "token_hash", nullable = false, length = 64)
    var tokenHash: String = "",

    @Column(name = "device_label", nullable = false, length = 255)
    var deviceLabel: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now(),

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,
)