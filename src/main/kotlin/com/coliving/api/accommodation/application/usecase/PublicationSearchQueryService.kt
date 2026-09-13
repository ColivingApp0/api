package com.coliving.api.accommodation.application.usecase

import com.coliving.api.accommodation.application.dto.PublishedListingInfo
import com.coliving.api.accommodation.application.query.PublicationSearchQuery
import com.coliving.api.accommodation.domain.enums.PublicationStatus
import com.coliving.api.accommodation.domain.repository.PropertyRepository
import com.coliving.api.accommodation.domain.repository.PublicationRepository
import com.coliving.api.accommodation.domain.repository.PricingRepository
import com.coliving.api.accommodation.domain.repository.RuleRepository
import com.coliving.api.accommodation.domain.repository.UnitRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read-side implementation of [PublicationSearchQuery]: a pure projection over
 * accommodation aggregates. No business decisions here — filtering and
 * ordering belong to the `search` context.
 */
@Service
class PublicationSearchQueryService(
    private val publicationRepository: PublicationRepository,
    private val unitRepository: UnitRepository,
    private val propertyRepository: PropertyRepository,
    private val pricingRepository: PricingRepository,
    private val ruleRepository: RuleRepository,
) : PublicationSearchQuery {

    @Transactional(readOnly = true)
    override fun findPublishedListings(): List<PublishedListingInfo> =
        publicationRepository.findAllPublished().map { publication ->
            val unit = unitRepository.findById(publication.unitId)
            val property = unit?.let { propertyRepository.findById(it.propertyId) }
            val pricing = pricingRepository.findByPublication(publication.id)
            val rule = ruleRepository.findByPublication(publication.id)
            PublishedListingInfo(
                publicationId = publication.id,
                unitId = publication.unitId,
                title = publication.title,
                cityId = property?.cityId,
                pricePerNight = pricing?.basePricePerNight,
                currency = pricing?.currency?.name,
                publishedAt = publication.updatedAt,
                serviceCodes = publication.services,
                roomTypeCode = unit?.typeCode,
                accessibilityCodes = unit?.accessibilityCodes ?: emptySet(),
                minNights = rule?.minNights,
            )
        }

    @Transactional(readOnly = true)
    override fun isPublished(publicationId: UUID): Boolean {
        val publication = publicationRepository.findById(publicationId) ?: return false
        return publication.status == PublicationStatus.PUBLICADA
    }
}
