package com.coliving.api.identity.domain.repository

import com.coliving.api.identity.domain.enums.ConsentType
import com.coliving.api.identity.domain.model.Consent
import java.util.UUID

/**
 * Out port for consents (RF-005). One record per (user, type).
 */
interface ConsentRepository {
    fun findByUserAndType(userId: UUID, type: ConsentType): Consent?
    fun findByUser(userId: UUID): List<Consent>
    fun save(consent: Consent)
}