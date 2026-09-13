package com.coliving.api.community.presentation.dto

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

/** Creates an activity for one of the host's properties (RF-060). */
data class CreateActivityRequest(
    @field:NotNull
    val propertyId: UUID,
    @field:NotBlank
    @field:Size(max = 120)
    val title: String,
    @field:Size(max = 1000)
    val description: String? = null,
    @field:NotNull
    @field:Future
    val scheduledAt: Instant,
    @field:Positive
    val capacity: Int,
    @field:NotEmpty
    val enabledParticipantIds: List<UUID>,
)