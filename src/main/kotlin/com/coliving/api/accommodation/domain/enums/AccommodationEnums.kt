package com.coliving.api.accommodation.domain.enums

/**
 * Lifecycle of a room listing (Publication). Only [PUBLICADA] rooms are
 * visible to guests and accept requests. [ARCHIVADA] is terminal.
 *
 * [EN_REVISION] is the moderation state (RF-081). PAUSADA is the host's
 * commercial switch: the room can have free places and still be paused.
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
 * Decisions of the moderation review of a publication (RF-081).
 */
enum class ReviewDecision {
    APROBADA,
    RECHAZADA,
    CORRECCION_SOLICITADA,
}

/** Billing unit of the monthly rent. */
enum class PriceUnit {
    POR_HABITACION,
    POR_PERSONA,
}

/** Whether the room is rented to one guest or shared by several. */
enum class RoomType {
    INDIVIDUAL,
    COMPARTIDA,
}

/** Bathroom available to the room. */
enum class BathroomType {
    PRIVADO,
    COMPARTIDO,
}

/** Supported pricing currencies. */
enum class Currency {
    COP,
    USD,
}
