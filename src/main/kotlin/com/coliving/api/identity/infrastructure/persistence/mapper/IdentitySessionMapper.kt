package com.coliving.api.identity.infrastructure.persistence.mapper

import com.coliving.api.identity.domain.model.SessionToken
import com.coliving.api.identity.infrastructure.persistence.entity.IdentitySessionEntity

object IdentitySessionMapper {

    fun toDomain(entity: IdentitySessionEntity): SessionToken = SessionToken(
        id = entity.id,
        userId = entity.userId,
        tokenHash = entity.tokenHash,
        deviceLabel = entity.deviceLabel,
        createdAt = entity.createdAt,
        expiresAt = entity.expiresAt,
        revokedAt = entity.revokedAt,
    )

    fun toEntity(token: SessionToken): IdentitySessionEntity = IdentitySessionEntity(
        id = token.id,
        userId = token.userId,
        tokenHash = token.tokenHash,
        deviceLabel = token.deviceLabel,
        createdAt = token.createdAt,
        expiresAt = token.expiresAt,
        revokedAt = token.revokedAt,
    )
}