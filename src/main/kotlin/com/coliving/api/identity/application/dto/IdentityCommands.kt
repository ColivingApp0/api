package com.coliving.api.identity.application.dto

import com.coliving.api.identity.domain.enums.ConsentType
import com.coliving.api.identity.domain.enums.DocumentType
import com.coliving.api.identity.domain.enums.RoleCode
import java.util.UUID

data class RegisterUserCommand(
    val email: String,
    val password: String,
)

data class LoginCommand(
    val email: String,
    val password: String,
    val deviceLabel: String = "unknown",
)

data class VerifyEmailCommand(
    val token: String,
)

data class RequestPasswordResetCommand(
    val email: String,
)

data class ResetPasswordCommand(
    val token: String,
    val newPassword: String,
)

data class SelectRoleCommand(
    val userId: UUID,
    val roleCode: RoleCode,
)

data class ManageConsentCommand(
    val userId: UUID,
    val type: ConsentType,
    val version: String,
    val purpose: String,
    val accept: Boolean,
)

/**
 * Not a `data` class on purpose: [bytes] must not participate in equals/hashCode.
 */
class SubmitDocumentCommand(
    val userId: UUID,
    val type: DocumentType,
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray,
)

enum class ReviewAction {
    APPROVE,
    REJECT,
    REQUEST_CORRECTION,
}

data class ReviewDocumentCommand(
    val documentId: UUID,
    val reviewerUserId: UUID,
    val action: ReviewAction,
    val reason: String? = null,
)

data class AssignRoleCommand(
    val userId: UUID,
    val roleCode: RoleCode,
)