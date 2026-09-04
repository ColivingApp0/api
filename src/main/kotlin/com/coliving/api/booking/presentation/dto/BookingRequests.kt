package com.coliving.api.booking.presentation.dto

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalDate

data class CreateReservationRequest(
    @field:NotNull
    val unitId: java.util.UUID,
    @field:NotNull
    @field:Future
    val fromDate: LocalDate,
    @field:NotNull
    @field:Future
    val toDate: LocalDate,
    @field:Positive
    val occupants: Int,
    val message: String? = null,
)

data class DecisionRequest(
    val reason: String? = null,
)

data class CancelRequest(
    @field:NotBlank
    val reason: String,
)
