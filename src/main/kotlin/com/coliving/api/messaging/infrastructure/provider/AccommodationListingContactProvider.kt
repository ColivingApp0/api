package com.coliving.api.messaging.infrastructure.provider

import com.coliving.api.accommodation.application.query.ListingContactQuery
import com.coliving.api.messaging.application.port.out.ListingContact
import com.coliving.api.messaging.application.port.out.ListingContactPort
import java.util.UUID
import org.springframework.stereotype.Component

/** Adapter over accommodation's ListingContactQuery (host of a room). */
@Component
class AccommodationListingContactProvider(
    private val listingContactQuery: ListingContactQuery,
) : ListingContactPort {

    override fun findListing(unitId: UUID): ListingContact? =
        listingContactQuery.findListing(unitId)?.let {
            ListingContact(
                unitId = it.unitId,
                publicationId = it.publicationId,
                hostId = it.hostId,
                roomTitle = it.roomTitle,
                propertyTitle = it.propertyTitle,
                cityName = it.cityName,
            )
        }
}
