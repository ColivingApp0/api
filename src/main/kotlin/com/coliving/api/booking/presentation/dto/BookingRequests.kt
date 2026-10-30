package com.coliving.api.booking.presentation.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.util.UUID

/** Monthly room request: no dates or nights, only the room and an optional message. */
data class CreateReservationRequest(
    @field:NotNull
    val unitId: UUID,
    @field:Positive
    val places: Int = 1,
    val message: String? = null,
)

data class DecisionRequest(
    val reason: String? = null,
)

data class CancelRequest(
    @field:NotBlank
    val reason: String,
)
