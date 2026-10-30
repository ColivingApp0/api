package com.coliving.api.booking.domain.enums

/**
 * Lifecycle of a monthly room request:
 *
 * ```
 * SOLICITADA ──> ACEPTADA      (host accepts; the place becomes occupied)
 *      ├────────> RECHAZADA    (host, terminal)
 *      └────────> CANCELADA    (guest or host, terminal)
 * ```
 *
 * INFORMACION_SOLICITADA is kept as an intermediate host decision. CONFIRMADA
 * and EXPIRADA remain only to hydrate historical nightly reservations; new
 * monthly requests never enter those states.
 */
enum class ReservationStatus {
    SOLICITADA,
    INFORMACION_SOLICITADA,
    ACEPTADA,
    CONFIRMADA,
    RECHAZADA,
    CANCELADA,
    EXPIRADA,
}

/** Types of history entries; every transition records actor and timestamp. */
enum class ReservationEventType {
    CREADA,
    INFORMACION_SOLICITADA,
    ACEPTADA,
    RECHAZADA,
    CONFIRMADA,
    CANCELADA,
    EXPIRADA,
}
