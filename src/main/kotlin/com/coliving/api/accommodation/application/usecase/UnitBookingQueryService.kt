package com.coliving.api.accommodation.application.usecase

import com.coliving.api.accommodation.application.dto.UnitBookingInfo
import com.coliving.api.accommodation.application.query.UnitBookingQuery
import com.coliving.api.accommodation.domain.enums.PublicationStatus
import com.coliving.api.accommodation.domain.repository.PricingRepository
import com.coliving.api.accommodation.domain.repository.PropertyRepository
import com.coliving.api.accommodation.domain.repository.PublicationRepository
import com.coliving.api.accommodation.domain.repository.RuleRepository
import com.coliving.api.accommodation.domain.repository.UnitRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read-side implementation of [UnitBookingQuery]. Pure projection over the
 * accommodation aggregates — no mutation, no business decisions: deciding
 * whether a publication admits requests is the booking context's job (RN-03).
 */
@Service
class UnitBookingQueryService(
    private val unitRepository: UnitRepository,
    private val propertyRepository: PropertyRepository,
    private val publicationRepository: PublicationRepository,
    private val pricingRepository: PricingRepository,
    private val ruleRepository: RuleRepository,
) : UnitBookingQuery {

    @Transactional(readOnly = true)
    override fun getUnitBookingInfo(unitId: UUID): UnitBookingInfo? {
        val unit = unitRepository.findById(unitId) ?: return null
        val publication = publicationRepository.findByUnit(unitId) ?: return null
        val pricing = pricingRepository.findByPublication(publication.id) ?: return null
        val rule = ruleRepository.findByPublication(publication.id)
        return UnitBookingInfo(
            unitId = unit.id,
            publicationId = publication.id,
            publicationStatus = publication.status.name,
            maxGuests = unit.maxGuests,
            basePricePerNight = pricing.basePricePerNight,
            currency = pricing.currency,
            cancellationPolicy = rule?.cancellationPolicy?.name,
        )
    }

    @Transactional(readOnly = true)
    override fun unitIdsOfHost(hostId: UUID): List<UUID> =
        propertyRepository.findByHost(hostId)
            .flatMap { property -> unitRepository.findByProperty(property.id) }
            .map { it.id }

    @Transactional(readOnly = true)
    override fun isHostOfUnit(unitId: UUID, hostId: UUID): Boolean {
        val unit = unitRepository.findById(unitId) ?: return false
        val property = propertyRepository.findById(unit.propertyId) ?: return false
        return property.hostId == hostId
    }
}
