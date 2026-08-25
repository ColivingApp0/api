package com.coliving.api.identity.infrastructure.persistence.entity

import com.coliving.api.identity.domain.enums.SecurityTokenPurpose
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "identity_security_token",
    indexes = [
        Index(name = "uk_identity_security_token_hash_purpose", columnList = "token_hash,purpose", unique = true),
    ],
)
class IdentitySecurityTokenEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 50)
    var purpose: SecurityTokenPurpose = SecurityTokenPurpose.EMAIL_VERIFICATION,

    @Column(name = "token_hash", nullable = false, length = 64)
    var tokenHash: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now(),

    @Column(name = "used_at")
    var usedAt: Instant? = null,
)