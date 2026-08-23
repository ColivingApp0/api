package com.coliving.api.shared.presentation.dto

/**
 * Uniform error body produced by the shared GlobalExceptionHandler.
 */
data class ErrorResponse(
    val code: String,
    val message: String,
    val fieldErrors: Map<String, String>? = null,
)