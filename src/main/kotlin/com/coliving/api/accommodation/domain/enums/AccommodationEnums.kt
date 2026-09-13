package com.coliving.api.accommodation.domain.enums

/**
 * Lifecycle of a listing (Publication). Only [PUBLICADA] listings are visible
 * to guests. [ARCHIVADA] is terminal.
 *
 * [EN_REVISION] is the moderation state (RF-081): a host can send a draft to
 * review before publishing (RF-022 "enviar a revisión") and the moderation
 * team can also take an already published listing into review. Resolution
 * (approve/reject/correction) returns the listing to the state recorded in
 * [Publication.reviewSourceStatus].
 */
enum class PublicationStatus {
    BORRADOR,
    EN_REVISION,
    PUBLICADA,
    PAUSADA,
    OCULTA,
    ARCHIVADA,
}

/**
 * Decisions of the moderation review of a publication (RF-081):
 * - APROBADA: publication is fit to be (or stay) public.
 * - RECHAZADA: content is not acceptable; drafts return to BORRADOR and
 *   published listings are hidden (OCULTA).
 * - CORRECCION_SOLICITADA: the host must correct the listing; always returns
 *   to BORRADOR. A note is mandatory for both non-approval decisions.
 */
enum class ReviewDecision {
    APROBADA,
    RECHAZADA,
    CORRECCION_SOLICITADA,
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