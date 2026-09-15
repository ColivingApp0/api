package com.coliving.api.moderation.domain.enums

/**
 * Origin of a support case (RF-082). A REPORTE comes from a user reporting
 * content or a conversation (RF-052); a DISPUTA comes from a user disputing an
 * evaluation that affects their score (RF-072). Both are managed with the same
 * lifecycle, so support handles one queue.
 */
enum class CaseType {
    REPORTE,
    DISPUTA,
}

/** What the case is about; the ids are cross-context references (no FK). */
enum class CaseSubjectType {
    CONVERSACION,
    EVALUACION,
    PUBLICACION,
    USUARIO,
}

/**
 * Triage priority of a case (RF-082). It is decided when the case is opened so
 * the support queue can be ordered and response times measured.
 */
enum class CasePriority {
    ALTA,
    MEDIA,
    BAJA,
}

/**
 * Lifecycle of a case (RF-082):
 *
 * ```
 * ABIERTO ──> EN_REVISION ──┬──> RESUELTO   (resolution recorded)
 *    └──────────────────────┴──> RECHAZADO  (reason recorded)
 * ```
 *
 * RESUELTO and RECHAZADO are terminal: every decision is recorded with the
 * responsible moderator and a mandatory note, which makes the case auditable.
 */
enum class CaseStatus {
    ABIERTO,
    EN_REVISION,
    RESUELTO,
    RECHAZADO,
}

/** Traceability entries: every action on a case leaves one (RF-082). */
enum class CaseEventType {
    ABIERTO,
    ASIGNADO,
    EN_REVISION,
    RESUELTO,
    RECHAZADO,
}