package com.coliving.api.community.domain.enums

/**
 * Lifecycle of a community activity (SRS RF-060). An activity is published to
 * its enabled participants when created and stays PROGRAMADA until the host
 * cancels it; "already happened" is derived from the scheduled instant, not
 * stored, so no scheduler is needed to keep the state truthful.
 */
enum class CommunityActivityStatus {
    PROGRAMADA,
    CANCELADA,
}

/**
 * Participation of a resident in an activity (RF-060, RF-061). The host enables
 * the residents of the property when creating the activity (HABILITADO); each
 * enabled resident confirms attendance (CONFIRMADA) — subject to capacity — or
 * withdraws (RETIRADA), and can confirm again while the activity is open.
 */
enum class ParticipationStatus {
    HABILITADO,
    CONFIRMADA,
    RETIRADA,
}