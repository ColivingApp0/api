package com.coliving.api.identity.domain.model

import com.coliving.api.identity.domain.enums.ConsentType
import java.time.Instant
import java.util.UUID

/**
 * Maps the diagram's `Consentimiento` entity. Keeps version, purpose, date and
 * acceptance state per type (RF-005). `accept`/`revoke` are the only ways to
 * change state, so the record is auditable.
 */
class Consent(
    val id: UUID,
    val userId: UUID,
    val type: ConsentType,
    version: String,
    purpose: String,
    accepted: Boolean,
    timestamp: Instant,
) {
    var version: String = version
        private set

    var purpose: String = purpose
        private set

    var accepted: Boolean = accepted
        private set

    var timestamp: Instant = timestamp
        private set

    fun accept(version: String, purpose: String, now: Instant) {
        this.version = version
        this.purpose = purpose
        this.accepted = true
        this.timestamp = now
    }

    fun revoke(now: Instant) {
        accepted = false
        timestamp = now
    }

    companion object {
        fun create(
            userId: UUID,
            type: ConsentType,
            version: String,
            purpose: String,
            now: Instant,
        ): Consent = Consent(
            id = UUID.randomUUID(),
            userId = userId,
            type = type,
            version = version,
            purpose = purpose,
            accepted = true,
            timestamp = now,
        )
    }
}