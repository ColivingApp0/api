package com.coliving.api.identity.presentation.dto

import com.coliving.api.identity.domain.enums.RoleCode
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "email is required")
    @field:Email(message = "email is invalid")
    val email: String = "",

    @field:NotBlank(message = "password is required")
    @field:Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
    val password: String = "",
)

data class LoginRequest(
    @field:NotBlank(message = "email is required")
    val email: String = "",

    @field:NotBlank(message = "password is required")
    val password: String = "",

    val deviceLabel: String = "unknown",
)

data class VerifyEmailRequest(
    @field:NotBlank(message = "token is required")
    val token: String = "",
)

data class RequestPasswordResetRequest(
    @field:NotBlank(message = "email is required")
    @field:Email(message = "email is invalid")
    val email: String = "",
)

data class ResetPasswordRequest(
    @field:NotBlank(message = "token is required")
    val token: String = "",

    @field:NotBlank(message = "newPassword is required")
    @field:Size(min = 8, max = 72, message = "newPassword must be between 8 and 72 characters")
    val newPassword: String = "",
)

data class SelectRoleRequest(
    @field:NotNull(message = "roleCode is required")
    val roleCode: RoleCode? = null,
)

data class ManageConsentRequest(
    @field:NotBlank(message = "version is required")
    val version: String = "",

    @field:NotBlank(message = "purpose is required")
    val purpose: String = "",

    @field:NotNull(message = "accept is required")
    val accept: Boolean? = null,
)

data class ReviewDocumentRequest(
    val reason: String? = null,
)

data class AssignRoleRequest(
    @field:NotNull(message = "roleCode is required")
    val roleCode: RoleCode? = null,
)