package com.coliving.api.shared.presentation.error

import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.DomainException
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.InvalidArgumentException
import com.coliving.api.shared.error.NotFoundException
import com.coliving.api.shared.error.UnauthorizedException
import com.coliving.api.shared.presentation.dto.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException

/**
 * Maps domain/application exceptions and bean-validation errors to a uniform
 * REST error body (RNF-002: authorization validated in the backend).
 * Cross-cutting concern, so it lives in `shared`: every bounded context uses
 * it without depending on `identity`.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    fun handle(e: NotFoundException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.NOT_FOUND, e)

    @ExceptionHandler(ConflictException::class)
    fun handle(e: ConflictException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.CONFLICT, e)

    @ExceptionHandler(ForbiddenException::class)
    fun handle(e: ForbiddenException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.FORBIDDEN, e)

    @ExceptionHandler(UnauthorizedException::class)
    fun handle(e: UnauthorizedException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.UNAUTHORIZED, e)

    @ExceptionHandler(InvalidArgumentException::class)
    fun handle(e: InvalidArgumentException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.BAD_REQUEST, e)

    @ExceptionHandler(DomainException::class)
    fun handle(e: DomainException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.BAD_REQUEST, e)

    @ExceptionHandler(AccessDeniedException::class)
    fun handle(e: AccessDeniedException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.FORBIDDEN, "FORBIDDEN", e.message ?: "Access denied")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val fieldErrors = e.bindingResult.fieldErrors
            .associate { it.field to (it.defaultMessage ?: "invalid") }
        return ResponseEntity.badRequest().body(
            ErrorResponse(
                code = "VALIDATION_ERROR",
                message = "Request validation failed",
                fieldErrors = fieldErrors,
            ),
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handle(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(
            ErrorResponse(
                code = "MALFORMED_REQUEST",
                message = "Request body could not be parsed",
            ),
        )

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handle(e: MaxUploadSizeExceededException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE", "File exceeds the allowed size")

    private fun respond(status: HttpStatus, e: DomainException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(
            ErrorResponse(
                code = e.errorCode,
                message = e.message ?: e.errorCode,
            ),
        )

    private fun respond(status: HttpStatus, code: String, message: String): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(ErrorResponse(code = code, message = message))
}