package com.coliving.api.accommodation.application.usecase

import com.coliving.api.accommodation.application.query.ListingContactInfo
import com.coliving.api.accommodation.application.query.ListingContactQuery
import com.coliving.api.accommodation.domain.repository.PropertyRepository
import com.coliving.api.accommodation.domain.repository.PublicationRepository
import com.coliving.api.accommodation.domain.repository.UnitRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ListingContactQueryService(
    private val unitRepository: UnitRepository,
    private val propertyRepository: PropertyRepository,
    private val publicationRepository: PublicationRepository,
) : ListingContactQuery {

    @Transactional(readOnly = true)
    override fun findListing(unitId: UUID): ListingContactInfo? {
        val unit = unitRepository.findById(unitId) ?: return null
        val property = propertyRepository.findById(unit.propertyId) ?: return null
        val publication = publicationRepository.findByUnit(unit.id) ?: return null
        return ListingContactInfo(
            unitId = unit.id,
            publicationId = publication.id,
            hostId = property.hostId,
            roomTitle = unit.name,
            propertyTitle = property.title ?: "",
            cityName = property.cityName,
        )
    }
}
