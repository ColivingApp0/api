package com.coliving.api.identity.presentation.controller

import com.coliving.api.identity.application.dto.LoginResult
import com.coliving.api.identity.application.dto.RegisterUserResult
import com.coliving.api.identity.application.dto.RequestPasswordResetCommand
import com.coliving.api.identity.application.dto.ResetPasswordCommand
import com.coliving.api.identity.application.dto.RegisterUserCommand
import com.coliving.api.identity.application.dto.LoginCommand
import com.coliving.api.identity.application.dto.VerifyEmailCommand
import com.coliving.api.identity.application.usecase.AuthenticateUserService
import com.coliving.api.identity.application.usecase.LogoutService
import com.coliving.api.identity.application.usecase.RegisterUserService
import com.coliving.api.identity.application.usecase.RequestPasswordResetService
import com.coliving.api.identity.application.usecase.ResetPasswordService
import com.coliving.api.identity.application.usecase.VerifyEmailService
import com.coliving.api.identity.presentation.dto.LoginRequest
import com.coliving.api.identity.presentation.dto.RegisterRequest
import com.coliving.api.identity.presentation.dto.RequestPasswordResetRequest
import com.coliving.api.identity.presentation.dto.ResetPasswordRequest
import com.coliving.api.identity.presentation.dto.VerifyEmailRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Public authentication endpoints (RF-001, RF-002, RF-003).
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val registerUserService: RegisterUserService,
    private val verifyEmailService: VerifyEmailService,
    private val authenticateUserService: AuthenticateUserService,
    private val logoutService: LogoutService,
    private val requestPasswordResetService: RequestPasswordResetService,
    private val resetPasswordService: ResetPasswordService,
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<RegisterUserResult> {
        val result = registerUserService.register(
            RegisterUserCommand(
                email = request.email,
                password = request.password,
            ),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun verifyEmail(@Valid @RequestBody request: VerifyEmailRequest) {
        verifyEmailService.verify(VerifyEmailCommand(token = request.token))
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResult =
        authenticateUserService.login(
            LoginCommand(
                email = request.email,
                password = request.password,
                deviceLabel = request.deviceLabel,
            ),
        )

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@RequestHeader("Authorization") authorization: String?) {
        val rawToken = authorization?.removePrefix("Bearer ")?.trim().orEmpty()
        logoutService.logout(rawToken)
    }

    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun requestPasswordReset(@Valid @RequestBody request: RequestPasswordResetRequest) {
        requestPasswordResetService.request(
            RequestPasswordResetCommand(email = request.email),
        )
    }

    @PostMapping("/password-reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest) {
        resetPasswordService.reset(
            ResetPasswordCommand(
                token = request.token,
                newPassword = request.newPassword,
            ),
        )
    }
}