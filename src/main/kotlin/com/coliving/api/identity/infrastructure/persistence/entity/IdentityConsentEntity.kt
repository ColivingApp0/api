package com.coliving.api.identity.infrastructure.persistence.entity

import com.coliving.api.identity.domain.enums.ConsentType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "identity_consent",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_identity_consent_user_type", columnNames = ["user_id", "type"]),
    ],
)
class IdentityConsentEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    var type: ConsentType = ConsentType.TERMINOS,

    @Column(name = "version", nullable = false, length = 100)
    var version: String = "",

    @Column(name = "purpose", nullable = false, length = 500)
    var purpose: String = "",

    @Column(name = "accepted", nullable = false)
    var accepted: Boolean = true,

    @Column(name = "accepted_at", nullable = false)
    var acceptedAt: Instant = Instant.now(),
)