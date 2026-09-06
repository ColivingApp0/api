package com.coliving.api.search.infrastructure.provider

import com.coliving.api.accommodation.application.query.PublicationSearchQuery
import com.coliving.api.search.application.port.out.ListingFacet
import com.coliving.api.search.application.port.out.PublicationCatalogPort
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Adapter from the search catalog port to accommodation's
 * PublicationSearchQuery — same cross-context pattern as the other contexts.
 */
@Component
class AccommodationSearchProvider(
    private val publicationSearchQuery: PublicationSearchQuery,
) : PublicationCatalogPort {

    override fun findPublishedListings(): List<ListingFacet> =
        publicationSearchQuery.findPublishedListings().map { info ->
            ListingFacet(
                publicationId = info.publicationId,
                unitId = info.unitId,
                title = info.title,
                cityId = info.cityId,
                pricePerNight = info.pricePerNight,
                currency = info.currency,
                updatedAt = info.publishedAt,
            )
        }

    override fun isPublished(publicationId: UUID): Boolean =
        publicationSearchQuery.isPublished(publicationId)
}
