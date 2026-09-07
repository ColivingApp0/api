package com.coliving.api.booking.application.query

import com.coliving.api.booking.application.dto.ReservationPartiesInfo
import java.util.UUID

/**
 * Read contract that the `messaging` bounded context consumes (through an
 * adapter) to resolve which two users may converse inside a reservation
 * (RF-050). Mirrors accommodation's `UnitBookingQuery`: the providing context
 * owns both the data and this projection — messaging never reads booking
 * tables.
 */
interface ReservationMessagingQuery {

    /**
     * Participants of the conversation attached to [reservationId], or null
     * when the reservation does not exist. Every reservation has exactly two
     * parties: the guest and the unit's host.
     */
    fun partiesOf(reservationId: UUID): ReservationPartiesInfo?
}