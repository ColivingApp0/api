package com.coliving.api.identity.infrastructure.persistence.mapper

import com.coliving.api.identity.domain.model.Consent
import com.coliving.api.identity.infrastructure.persistence.entity.IdentityConsentEntity

object IdentityConsentMapper {

    fun toDomain(entity: IdentityConsentEntity): Consent = Consent(
        id = entity.id,
        userId = entity.userId,
        type = entity.type,
        version = entity.version,
        purpose = entity.purpose,
        accepted = entity.accepted,
        timestamp = entity.acceptedAt,
    )

    fun toEntity(consent: Consent): IdentityConsentEntity = IdentityConsentEntity(
        id = consent.id,
        userId = consent.userId,
        type = consent.type,
        version = consent.version,
        purpose = consent.purpose,
        accepted = consent.accepted,
        acceptedAt = consent.timestamp,
    )
}