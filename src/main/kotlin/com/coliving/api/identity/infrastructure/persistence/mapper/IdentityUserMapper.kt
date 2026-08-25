package com.coliving.api.identity.infrastructure.persistence.mapper

import com.coliving.api.identity.domain.model.User
import com.coliving.api.identity.domain.vo.Email
import com.coliving.api.identity.domain.vo.PasswordHash
import com.coliving.api.identity.infrastructure.persistence.entity.IdentityUserEntity
import java.time.Instant
import java.util.UUID

object IdentityUserMapper {

    fun toDomain(
        id: UUID,
        email: String,
        passwordHash: String,
        status: com.coliving.api.identity.domain.enums.UserStatus,
        emailVerified: Boolean,
        createdAt: Instant,
        roles: Set<com.coliving.api.identity.domain.model.UserRole>,
    ): User = User.restore(
        id = id,
        email = Email.of(email),
        passwordHash = PasswordHash.of(passwordHash),
        status = status,
        emailVerified = emailVerified,
        createdAt = createdAt,
        roles = roles,
    )

    fun toEntity(user: User): IdentityUserEntity = IdentityUserEntity(
        id = user.id,
        email = user.email.value,
        passwordHash = user.passwordHash.value,
        status = user.status,
        emailVerified = user.emailVerified,
        createdAt = user.createdAt,
        updatedAt = Instant.now(),
    )
}