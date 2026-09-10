package com.coliving.api.booking.infrastructure.provider

import com.coliving.api.accommodation.application.query.UnitBookingQuery
import com.coliving.api.booking.application.port.out.BookableUnit
import com.coliving.api.booking.application.port.out.UnitCatalogPort
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Adapter from booking's catalog port to accommodation's UnitBookingQuery —
 * same cross-context pattern as identity's UserVerificationQuery consumers.
 */
@Component
class AccommodationCatalogProvider(
    private val unitBookingQuery: UnitBookingQuery,
) : UnitCatalogPort {

    override fun findBookableUnit(unitId: UUID): BookableUnit? =
        unitBookingQuery.getUnitBookingInfo(unitId)?.let { info ->
            BookableUnit(
                unitId = info.unitId,
                publicationId = info.publicationId,
                status = info.publicationStatus,
                maxGuests = info.maxGuests,
                basePricePerNight = info.basePricePerNight,
                currency = info.currency.name,
                cancellationPolicy = info.cancellationPolicy,
            )
        }

    override fun unitIdsOfHost(hostId: UUID): List<UUID> = unitBookingQuery.unitIdsOfHost(hostId)

    override fun isHostOfUnit(unitId: UUID, hostId: UUID): Boolean =
        unitBookingQuery.isHostOfUnit(unitId, hostId)

    override fun hostOfUnit(unitId: UUID): UUID? = unitBookingQuery.hostOfUnit(unitId)
}
