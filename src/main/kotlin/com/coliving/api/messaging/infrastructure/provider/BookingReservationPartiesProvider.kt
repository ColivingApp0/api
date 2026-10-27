package com.coliving.api.messaging.infrastructure.provider

import com.coliving.api.booking.application.query.ReservationMessagingQuery
import com.coliving.api.messaging.application.port.out.ReservationParties
import com.coliving.api.messaging.application.port.out.ReservationPartiesPort
import java.util.UUID
import org.springframework.stereotype.Component

/** Adapter over booking's ReservationMessagingQuery. */
@Component
class BookingReservationPartiesProvider(
    private val reservationMessagingQuery: ReservationMessagingQuery,
) : ReservationPartiesPort {

    override fun findParties(reservationId: UUID): ReservationParties? =
        reservationMessagingQuery.partiesOf(reservationId)?.let {
            ReservationParties(
                reservationId = it.reservationId,
                unitId = it.unitId,
                publicationId = it.publicationId,
                guestUserId = it.guestUserId,
                hostUserId = it.hostUserId,
            )
        }
}
