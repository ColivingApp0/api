package com.coliving.api.messaging.presentation.dto

import com.coliving.api.messaging.domain.enums.PushPlatform
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * Opens (or reopens) a conversation. Exactly one of [reservationId] or
 * [unitId] must be provided: a reservation thread or a listing question.
 */
data class OpenConversationRequest(
    val reservationId: UUID? = null,
    val unitId: UUID? = null,
)

/** Payload of a message; content limits mirror the domain rule (RF-050). */
data class SendMessageRequest(
    @field:NotBlank
    @field:Size(max = 2000)
    val body: String,
)

/** Reports a conversation to support (RF-052); the reason is mandatory. */
data class ReportConversationRequest(
    @field:NotBlank
    @field:Size(max = 1000)
    val reason: String,
)

/** Blocks a user (RF-053). */
data class BlockUserRequest(
    @field:NotNull
    val blockedUserId: UUID,
)

/** FCM registration token of the current device (RF-051 external push). */
data class PushTokenRequest(
    @field:NotBlank
    @field:Size(max = 512)
    val token: String,
    @field:NotNull
    val platform: PushPlatform,
)
