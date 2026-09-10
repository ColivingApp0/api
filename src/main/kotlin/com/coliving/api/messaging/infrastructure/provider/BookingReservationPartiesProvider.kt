package com.coliving.api.messaging.infrastructure.provider

import com.coliving.api.booking.application.query.ReservationMessagingQuery
import com.coliving.api.messaging.application.port.out.ReservationParties
import com.coliving.api.messaging.application.port.out.ReservationPartiesPort
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Adapter over booking's [ReservationMessagingQuery]: messaging resolves the two
 * conversation parties (and the stay dates shown in the thread header) without
 * owning or reading reservation data.
 */
@Component
class BookingReservationPartiesProvider(
    private val reservationMessagingQuery: ReservationMessagingQuery,
) : ReservationPartiesPort {

    override fun findParties(reservationId: UUID): ReservationParties? =
        reservationMessagingQuery.partiesOf(reservationId)?.let {
            ReservationParties(
                reservationId = it.reservationId,
                guestUserId = it.guestUserId,
                hostUserId = it.hostUserId,
                fromDate = it.fromDate,
                toDate = it.toDate,
            )
        }
}