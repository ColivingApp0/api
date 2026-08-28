package com.coliving.api.accommodation.domain.enums

/**
 * Lifecycle of a listing (Publication). Only [PUBLICADA] listings are visible
 * to guests. [ARCHIVADA] is terminal.
 */
enum class PublicationStatus {
    BORRADOR,
    PUBLICADA,
    PAUSADA,
    OCULTA,
    ARCHIVADA,
}

/**
 * Per-day availability state of a unit. `DISPONIBLE` is the implicit default:
 * absence of a row means the day is free.
 *
 * - [BLOQUEADO] with a non-null reservation_id: a reservation is being processed.
 * - [BLOQUEADO] with null reservation_id: a host/manual block.
 * - [OCUPADO]: confirmed reservation.
 */
enum class AvailabilityState {
    DISPONIBLE,
    BLOQUEADO,
    OCUPADO,
}

/**
 * Cancellation policy options (Rule).
 */
enum class CancellationPolicy {
    FLEXIBLE,
    MODERADA,
    ESTRICTA,
}

/**
 * Supported pricing currencies.
 */
enum class Currency {
    COP,
    USD,
}