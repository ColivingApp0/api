package com.coliving.api.search.infrastructure.provider

import com.coliving.api.accommodation.application.query.PublicationSearchQuery
import com.coliving.api.search.application.port.out.ListingFacet
import com.coliving.api.search.application.port.out.PublicationCatalogPort
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class AccommodationSearchProvider(
    private val publicationSearchQuery: PublicationSearchQuery,
) : PublicationCatalogPort {

    override fun findPublishedListings(): List<ListingFacet> =
        publicationSearchQuery.findPublishedListings().map { info ->
            ListingFacet(
                publicationId = info.publicationId,
                unitId = info.unitId,
                propertyId = info.propertyId,
                roomTitle = info.roomTitle,
                propertyTitle = info.propertyTitle,
                description = info.description,
                cityId = info.cityId,
                cityName = info.cityName,
                neighborhood = info.neighborhood,
                monthlyPrice = info.monthlyPrice,
                currency = info.currency,
                priceUnit = info.priceUnit,
                roomType = info.roomType,
                bathroomType = info.bathroomType,
                airConditioning = info.airConditioning,
                fan = info.fan,
                capacity = info.capacity,
                occupiedPlaces = info.occupiedPlaces,
                availablePlaces = info.availablePlaces,
                photoUrl = info.photoUrl,
                propertyPhotoUrl = info.propertyPhotoUrl,
                updatedAt = info.publishedAt,
                serviceCodes = info.serviceCodes,
                accessibilityCodes = info.accessibilityCodes,
            )
        }

    override fun isPublished(publicationId: UUID): Boolean =
        publicationSearchQuery.isPublished(publicationId)
}
