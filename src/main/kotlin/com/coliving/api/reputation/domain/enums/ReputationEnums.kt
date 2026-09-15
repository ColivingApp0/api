package com.coliving.api.reputation.domain.enums

/**
 * Categories an evaluation is made of (RF-070: "bajo categorías definidas").
 * Every category is rated 1..5 and the overall rating of the evaluation is
 * derived from them, so the score always rests on the same defined set.
 */
enum class EvaluationCategory {
    LIMPIEZA,
    COMUNICACION,
    EXACTITUD,
    RESPETO,
    CUMPLIMIENTO,
}

/**
 * Lifecycle of an evaluation. ACTIVA counts for the score; DISPUTADA suspended
 * its effect until the case is resolved (RF-072: "suspende efectos cuando
 * corresponda").
 */
enum class EvaluationStatus {
    ACTIVA,
    DISPUTADA,
}

/**
 * Eligible relation that allows two users to evaluate each other (RF-070): a
 * confirmed stay. Kept as its own enum (instead of importing booking's) so both
 * contexts evolve independently, as booking does with the cancellation policy.
 */
enum class RelationType {
    ESTANCIA,
    INTERACCION,
}