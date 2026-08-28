package com.coliving.api.identity.infrastructure.persistence.mapper

import com.coliving.api.identity.domain.model.SecurityToken
import com.coliving.api.identity.infrastructure.persistence.entity.IdentitySecurityTokenEntity

object IdentitySecurityTokenMapper {

    fun toDomain(entity: IdentitySecurityTokenEntity): SecurityToken = SecurityToken(
        id = entity.id,
        userId = entity.userId,
        purpose = entity.purpose,
        tokenHash = entity.tokenHash,
        createdAt = entity.createdAt,
        expiresAt = entity.expiresAt,
        usedAt = entity.usedAt,
    )

    fun toEntity(token: SecurityToken): IdentitySecurityTokenEntity = IdentitySecurityTokenEntity(
        id = token.id,
        userId = token.userId,
        purpose = token.purpose,
        tokenHash = token.tokenHash,
        createdAt = token.createdAt,
        expiresAt = token.expiresAt,
        usedAt = token.usedAt,
    )
}