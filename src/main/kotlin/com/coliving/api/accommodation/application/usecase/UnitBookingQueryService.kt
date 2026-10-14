package com.coliving.api.accommodation.application.usecase

import com.coliving.api.accommodation.application.dto.UnitBookingInfo
import com.coliving.api.accommodation.application.query.UnitBookingQuery
import com.coliving.api.accommodation.domain.model.Unit
import com.coliving.api.accommodation.domain.repository.PropertyPhotoRepository
import com.coliving.api.accommodation.domain.repository.PropertyRepository
import com.coliving.api.accommodation.domain.repository.PublicationRepository
import com.coliving.api.accommodation.domain.repository.UnitPhotoRepository
import com.coliving.api.accommodation.domain.repository.UnitRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read-side implementation of [UnitBookingQuery]. Pure projection over the
 * accommodation aggregates; deciding whether a publication admits requests is
 * the booking context's job (RN-03).
 */
@Service
class UnitBookingQueryService(
    private val unitRepository: UnitRepository,
    private val propertyRepository: PropertyRepository,
    private val publicationRepository: PublicationRepository,
    private val unitPhotoRepository: UnitPhotoRepository,
    private val propertyPhotoRepository: PropertyPhotoRepository,
) : UnitBookingQuery {

    @Transactional(readOnly = true)
    override fun getUnitBookingInfo(unitId: UUID): UnitBookingInfo? {
        val unit = unitRepository.findById(unitId) ?: return null
        return toInfo(unit)
    }

    @Transactional
    override fun lockUnitBookingInfo(unitId: UUID): UnitBookingInfo? {
        val unit = unitRepository.findByIdForUpdate(unitId) ?: return null
        return toInfo(unit)
    }

    @Transactional(readOnly = true)
    override fun unitIdsOfHost(hostId: UUID): List<UUID> =
        propertyRepository.findByHost(hostId)
            .flatMap { property -> unitRepository.findByProperty(property.id) }
            .map { it.id }

    @Transactional(readOnly = true)
    override fun isHostOfUnit(unitId: UUID, hostId: UUID): Boolean =
        hostOfUnit(unitId) == hostId

    @Transactional(readOnly = true)
    override fun hostOfUnit(unitId: UUID): UUID? {
        val unit = unitRepository.findById(unitId) ?: return null
        val property = propertyRepository.findById(unit.propertyId) ?: return null
        return property.hostId
    }

    private fun toInfo(unit: Unit): UnitBookingInfo? {
        val publication = publicationRepository.findByUnit(unit.id) ?: return null
        val property = propertyRepository.findById(unit.propertyId) ?: return null
        val photoUrl = unitPhotoRepository.findByUnit(unit.id).firstOrNull()?.url
            ?: propertyPhotoRepository.findByProperty(property.id).firstOrNull()?.url
        return UnitBookingInfo(
            unitId = unit.id,
            propertyId = property.id,
            publicationId = publication.id,
            publicationStatus = publication.status.name,
            unitName = unit.name,
            propertyTitle = property.title ?: "",
            cityName = property.cityName,
            neighborhood = property.neighborhood,
            photoUrl = photoUrl,
            capacity = unit.maxGuests,
            monthlyPrice = unit.monthlyPrice,
            currency = unit.currency,
            priceUnit = unit.priceUnit,
            hostId = property.hostId,
        )
    }
}
