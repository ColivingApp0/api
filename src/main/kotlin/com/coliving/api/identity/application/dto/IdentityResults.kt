package com.coliving.api.identity.application.dto

import com.coliving.api.identity.domain.enums.ConsentType
import com.coliving.api.identity.domain.enums.DocumentStatus
import com.coliving.api.identity.domain.enums.DocumentType
import com.coliving.api.identity.domain.enums.RoleCode
import com.coliving.api.identity.domain.enums.UserStatus
import java.time.Instant
import java.util.UUID

data class RegisterUserResult(
    val userId: UUID,
    val status: UserStatus,
    val emailVerified: Boolean,
)

data class LoginUserSummary(
    val userId: UUID,
    val email: String,
    val emailVerified: Boolean,
    val status: UserStatus,
    val activeRoles: List<RoleSummary>,
)

data class LoginResult(
    val token: String,
    val expiresAt: Instant,
    val user: LoginUserSummary,
)

data class RoleSummary(
    val id: UUID,
    val code: RoleCode,
    val name: String,
)

data class UserRoleSummary(
    val id: UUID,
    val roleId: UUID,
    val code: RoleCode,
    val name: String,
    val active: Boolean,
    val assignedAt: Instant,
)

data class SelectRoleResult(
    val userId: UUID,
    val activeRole: UserRoleSummary,
)

data class ConsentSummary(
    val type: ConsentType,
    val version: String,
    val purpose: String,
    val accepted: Boolean,
    val timestamp: Instant,
)

data class DocumentSummary(
    val id: UUID,
    val type: DocumentType,
    val status: DocumentStatus,
    val uploadedAt: Instant,
    val reviewedAt: Instant?,
    val reviewReason: String?,
)

data class CurrentUserResult(
    val userId: UUID,
    val email: String,
    val emailVerified: Boolean,
    val status: UserStatus,
    val roles: List<UserRoleSummary>,
    val activeRole: UserRoleSummary?,
    val consents: List<ConsentSummary>,
    val documents: List<DocumentSummary>,
)