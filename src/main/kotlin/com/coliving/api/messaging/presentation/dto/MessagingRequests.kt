package com.coliving.api.messaging.presentation.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

/** Opens (or reopens) the single conversation attached to a reservation (RF-050). */
data class OpenConversationRequest(
    @field:NotBlank
    val reservationId: UUID,
)

/** Payload of a message; content limits mirror the domain rule (RF-050). */
data class SendMessageRequest(
    @field:NotBlank
    @field:Size(max = 2000)
    val body: String,
)