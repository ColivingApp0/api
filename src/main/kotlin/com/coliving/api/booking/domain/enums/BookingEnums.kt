package com.coliving.api.booking.domain.enums

/**
 * Lifecycle of a reservation (SRS RF-042). Transitions are only allowed
 * through the domain methods of [com.coliving.api.booking.domain.model.Reservation]:
 *
 * ```
 * SOLICITADA ──> INFORMACION_SOLICITADA ──┐
 *      │  ▲                               │ (host accepts)
 *      │  └── (host keeps deciding)  <─────┘
 *      ├──> ACEPTADA ──> CONFIRMADA        (guest confirms inside the hold window)
 *      ├──> RECHAZADA                      (host, terminal)
 *      ├──> CANCELADA                      (guest or host, terminal, reason required)
 *      └──> EXPIRADA                       (hold window elapsed, terminal)
 * ```
 *
 * ACEPTADA holds the unit's days (BLOQUEADO) until CONFIRMADA (OCUPADO) or
 * released (CANCELADA/EXPIRADA/RECHAZADA).
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

/** Types of history entries; every transition records actor and timestamp (RF-042). */
enum class ReservationEventType {
    CREADA,
    INFORMACION_SOLICITADA,
    ACEPTADA,
    RECHAZADA,
    CONFIRMADA,
    CANCELADA,
    EXPIRADA,
}

/**
 * Cancellation policy snapshot. Booking keeps its own copy (RN-07: the
 * conditions of a request are frozen in a snapshot) instead of importing the
 * accommodation enum, so both contexts evolve independently.
 */
enum class CancellationPolicyType {
    FLEXIBLE,
    MODERADA,
    ESTRICTA,
}
