package com.coliving.api.shared.error

/**
 * Base exception for domain and application errors within the modular backend.
 * Carries a stable machine-readable [errorCode] plus a human-readable message.
 */
open class DomainException(
    val errorCode: String,
    message: String,
) : RuntimeException(message)

class NotFoundException(message: String) : DomainException("NOT_FOUND", message)

class ConflictException(message: String) : DomainException("CONFLICT", message)

class ForbiddenException(message: String) : DomainException("FORBIDDEN", message)

class InvalidArgumentException(message: String) : DomainException("INVALID_ARGUMENT", message)

class UnauthorizedException(message: String) : DomainException("UNAUTHORIZED", message)