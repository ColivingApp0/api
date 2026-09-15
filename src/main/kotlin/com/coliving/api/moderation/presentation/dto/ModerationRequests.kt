package com.coliving.api.moderation.presentation.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** Closes a case with the mandatory resolution note (RF-082). */
data class ResolveCaseRequest(
    @field:NotBlank
    @field:Size(max = 1000)
    val resolution: String,
)

/** Closes a case as rejected with the mandatory reason (RF-082). */
data class RejectCaseRequest(
    @field:NotBlank
    @field:Size(max = 1000)
    val reason: String,
)