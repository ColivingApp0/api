package com.coliving.api.booking.infrastructure.provider

import com.coliving.api.accommodation.application.query.UnitBookingQuery
import com.coliving.api.booking.application.port.out.BookableUnit
import com.coliving.api.booking.application.port.out.UnitCatalogPort
import java.util.UUID
import org.springframework.stereotype.Component

/** Adapter from booking's catalog port to accommodation's UnitBookingQuery. */
@Component
class AccommodationCatalogProvider(
    private val unitBookingQuery: UnitBookingQuery,
) : UnitCatalogPort {

    override fun findBookableUnit(unitId: UUID): BookableUnit? =
        unitBookingQuery.getUnitBookingInfo(unitId)?.toBookable()

    override fun lockBookableUnit(unitId: UUID): BookableUnit? =
        unitBookingQuery.lockUnitBookingInfo(unitId)?.toBookable()

    override fun unitIdsOfHost(hostId: UUID): List<UUID> = unitBookingQuery.unitIdsOfHost(hostId)

    override fun isHostOfUnit(unitId: UUID, hostId: UUID): Boolean =
        unitBookingQuery.isHostOfUnit(unitId, hostId)

    override fun hostOfUnit(unitId: UUID): UUID? = unitBookingQuery.hostOfUnit(unitId)

    private fun com.coliving.api.accommodation.application.dto.UnitBookingInfo.toBookable() =
        BookableUnit(
            unitId = unitId,
            propertyId = propertyId,
            publicationId = publicationId,
            status = publicationStatus,
            unitName = unitName,
            propertyTitle = propertyTitle,
            cityName = cityName,
            neighborhood = neighborhood,
            photoUrl = photoUrl,
            capacity = capacity,
            monthlyPrice = monthlyPrice,
            currency = currency,
            priceUnit = priceUnit,
            hostId = hostId,
        )
}
