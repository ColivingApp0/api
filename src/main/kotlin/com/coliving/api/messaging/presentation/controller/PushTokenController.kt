package com.coliving.api.messaging.presentation.controller

import com.coliving.api.messaging.application.dto.PushTokenView
import com.coliving.api.messaging.application.dto.RegisterPushTokenCommand
import com.coliving.api.messaging.application.usecase.PushTokenService
import com.coliving.api.messaging.presentation.dto.PushTokenRequest
import com.coliving.api.shared.security.CurrentUser
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Device registration for the external push channel (RF-051). Any authenticated
 * user registers the FCM token of their own browser/app; the token is never
 * echoed back.
 */
@Tag(
    name = "Push tokens",
    description = "Registration of FCM device tokens of the authenticated user (RF-051 external push channel).",
)
@RestController
@RequestMapping("/api/v1/users/me/push-tokens")
class PushTokenController(
    private val pushTokenService: PushTokenService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @AuthenticationPrincipal current: CurrentUser,
        @Valid @RequestBody request: PushTokenRequest,
    ): PushTokenView =
        pushTokenService.register(
            current.userId,
            RegisterPushTokenCommand(token = request.token, platform = request.platform),
        )

    /** Idempotent; only the owner of the token can remove it. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unregister(
        @AuthenticationPrincipal current: CurrentUser,
        @RequestParam token: String,
    ) {
        pushTokenService.unregister(current.userId, token)
    }
}
