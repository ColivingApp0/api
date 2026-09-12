package com.coliving.api.community.infrastructure.provider

import com.coliving.api.booking.application.query.ReservationCommunityQuery
import com.coliving.api.community.application.port.out.CommunityResidencyPort
import com.coliving.api.community.application.port.out.PropertyCommunityPort
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Adapter over booking's [ReservationCommunityQuery]: a resident of a property
 * is a guest whose reservation over one of the property's units reached
 * CONFIRMADA (RF-061). The unit ids come from accommodation, so this context
 * composes the two read contracts instead of duplicating ownership data.
 */
@Component
class BookingResidencyProvider(
    private val propertyCommunityPort: PropertyCommunityPort,
    private val reservationCommunityQuery: ReservationCommunityQuery,
) : CommunityResidencyPort {

    override fun residentIdsOfProperty(propertyId: UUID): List<UUID> =
        reservationCommunityQuery.confirmedGuestIds(propertyCommunityPort.unitIdsOfProperty(propertyId))
}