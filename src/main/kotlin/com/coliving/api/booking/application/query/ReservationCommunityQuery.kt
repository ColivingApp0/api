package com.coliving.api.booking.application.query

import java.util.UUID

/**
 * Read contract that the `community` bounded context consumes (through an
 * adapter) to resolve who is a resident of a property (RF-061): the guests of
 * reservations that reached CONFIRMADA. Mirrors `ReservationMessagingQuery` —
 * the providing context owns the data and the projection.
 */
interface ReservationCommunityQuery {

    /**
     * Distinct guests whose reservation over any of [unitIds] is CONFIRMADA,
     * i.e. the residents of the property the units belong to. Empty when there
     * are no units or no confirmed stay.
     */
    fun confirmedGuestIds(unitIds: List<UUID>): List<UUID>
}