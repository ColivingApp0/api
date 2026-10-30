package com.coliving.api.accommodation.application.usecase

import com.coliving.api.accommodation.application.dto.PublishedListingInfo
import com.coliving.api.accommodation.application.port.out.UnitOccupancyPort
import com.coliving.api.accommodation.application.query.PublicationSearchQuery
import com.coliving.api.accommodation.domain.enums.PublicationStatus
import com.coliving.api.accommodation.domain.repository.PropertyPhotoRepository
import com.coliving.api.accommodation.domain.repository.PropertyRepository
import com.coliving.api.accommodation.domain.repository.PublicationRepository
import com.coliving.api.accommodation.domain.repository.UnitPhotoRepository
import com.coliving.api.accommodation.domain.repository.UnitRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read-side implementation of [PublicationSearchQuery]: a pure projection over
 * accommodation aggregates. Filtering and ordering belong to `search`.
 */
@Service
class PublicationSearchQueryService(
    private val publicationRepository: PublicationRepository,
    private val unitRepository: UnitRepository,
    private val propertyRepository: PropertyRepository,
    private val unitPhotoRepository: UnitPhotoRepository,
    private val propertyPhotoRepository: PropertyPhotoRepository,
    private val occupancyPort: UnitOccupancyPort,
) : PublicationSearchQuery {

    @Transactional(readOnly = true)
    override fun findPublishedListings(): List<PublishedListingInfo> {
        val publications = publicationRepository.findAllPublished()
        val units = publications.mapNotNull { publication ->
            unitRepository.findById(publication.unitId)?.let { publication to it }
        }
        val occupancy = occupancyPort.occupiedPlacesOf(units.map { it.second.id })
        return units.mapNotNull { (publication, unit) ->
            val property = propertyRepository.findById(unit.propertyId) ?: return@mapNotNull null
            if (property.archived) return@mapNotNull null
            val occupied = occupancy[unit.id] ?: 0
            PublishedListingInfo(
                publicationId = publication.id,
                unitId = unit.id,
                propertyId = property.id,
                roomTitle = unit.name,
                propertyTitle = property.title ?: "",
                description = unit.description,
                cityId = property.cityId,
                cityName = property.cityName,
                neighborhood = property.neighborhood,
                monthlyPrice = unit.monthlyPrice,
                currency = unit.currency.name,
                priceUnit = unit.priceUnit.name,
                roomType = unit.roomType.name,
                bathroomType = unit.bathroomType.name,
                airConditioning = unit.airConditioning,
                fan = unit.fan,
                capacity = unit.maxGuests,
                occupiedPlaces = occupied,
                availablePlaces = unit.availablePlaces(occupied),
                photoUrl = unitPhotoRepository.findByUnit(unit.id).firstOrNull()?.url,
                propertyPhotoUrl = propertyPhotoRepository.findByProperty(property.id).firstOrNull()?.url,
                publishedAt = publication.updatedAt,
                serviceCodes = publication.services,
                accessibilityCodes = unit.accessibilityCodes,
            )
        }
    }

    @Transactional(readOnly = true)
    override fun isPublished(publicationId: UUID): Boolean {
        val publication = publicationRepository.findById(publicationId) ?: return false
        return publication.status == PublicationStatus.PUBLICADA
    }
}
