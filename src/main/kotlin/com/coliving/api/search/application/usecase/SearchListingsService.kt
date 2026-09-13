package com.coliving.api.search.application.usecase

import com.coliving.api.search.application.dto.ListingSort
import com.coliving.api.search.application.dto.ListingView
import com.coliving.api.search.application.dto.SearchListingsCommand
import com.coliving.api.search.application.port.out.ListingFacet
import com.coliving.api.search.application.port.out.PublicationCatalogPort
import com.coliving.api.search.application.port.out.UnitAvailabilityPort
import com.coliving.api.shared.error.InvalidArgumentException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Public listing search (SRS group "Búsqueda").
 *
 * - RF-030 (Esencial): filters by city, dates and budget; results match the
 *   criteria and reflect the known availability state.
 * - RF-032 (Importante): ordering by price or recency, deterministic for the
 *   same query and data version (ties are broken by publicationId).
 *
 * Filters supported by the current data model. Sector / nearby-institution /
 * room-type facets (RF-030 partial, RF-031) stay pending until those catalogs
 * exist; affinity indicators (RF-035) depend on reputation.
 */
@Service
class SearchListingsService(
    private val publicationCatalogPort: PublicationCatalogPort,
    private val unitAvailabilityPort: UnitAvailabilityPort,
) {

    @Transactional(readOnly = true)
    fun search(command: SearchListingsCommand): List<ListingView> {
        validate(command)

        var listings = publicationCatalogPort.findPublishedListings()

        command.cityId?.let { cityId ->
            listings = listings.filter { it.cityId == cityId }
        }
        if (command.minPrice != null || command.maxPrice != null || command.currency != null) {
            listings = listings.filter { facet ->
                val price = facet.pricePerNight ?: return@filter false
                if (command.currency != null && facet.currency != command.currency) return@filter false
                val min = command.minPrice
                val max = command.maxPrice
                (min == null || price >= min) && (max == null || price <= max)
            }
        }
        if (command.availableFrom != null && command.availableTo != null) {
            listings = listings.filter { facet ->
                unitAvailabilityPort.isRangeAvailable(facet.unitId, command.availableFrom!!, command.availableTo!!)
            }
        }
        // Catalog-based filters (RF-031): services, room type, accessibility
        // and the minimum stay a listing admits.
        command.services?.takeIf { it.isNotEmpty() }?.let { requested ->
            listings = listings.filter { facet ->
                facet.serviceCodes.containsAll(requested)
            }
        }
        command.roomTypeCode?.let { roomType ->
            listings = listings.filter { it.roomTypeCode == roomType }
        }
        command.accessibilityCode?.let { accessibility ->
            listings = listings.filter { accessibility in it.accessibilityCodes }
        }
        command.minNights?.let { minimum ->
            listings = listings.filter { facet ->
                // A listing without configured rules admits any stay.
                (facet.minNights ?: 1) <= minimum
            }
        }

        val sorted = when (command.sort) {
            ListingSort.PRICE_ASC -> listings.sortedWith(
                compareBy<ListingFacet> { it.pricePerNight ?: BigDecimal.valueOf(Long.MAX_VALUE) }
                    .thenBy { it.publicationId },
            )
            ListingSort.PRICE_DESC -> listings.sortedWith(
                compareByDescending<ListingFacet> { it.pricePerNight ?: BigDecimal.ZERO }
                    .thenBy { it.publicationId },
            )
            ListingSort.RECENT -> listings.sortedWith(
                compareByDescending<ListingFacet> { it.updatedAt }.thenBy { it.publicationId },
            )
        }
        return sorted.map { facet ->
            ListingView(
                publicationId = facet.publicationId,
                unitId = facet.unitId,
                title = facet.title,
                cityId = facet.cityId,
                pricePerNight = facet.pricePerNight,
                currency = facet.currency,
                serviceCodes = facet.serviceCodes,
                roomTypeCode = facet.roomTypeCode,
                accessibilityCodes = facet.accessibilityCodes,
                minNights = facet.minNights,
            )
        }
    }

    private fun validate(command: SearchListingsCommand) {
        val from = command.availableFrom
        val to = command.availableTo
        if ((from == null) != (to == null)) {
            throw InvalidArgumentException("availableFrom and availableTo must be provided together")
        }
        if (from != null && to != null && to <= from) {
            throw InvalidArgumentException("availableTo must be after availableFrom")
        }
        if (from != null && ChronoUnit.DAYS.between(LocalDate.now(), to) > 366) {
            throw InvalidArgumentException("Availability range too large (max 366 nights)")
        }
        command.minPrice?.let { if (it.signum() < 0) throw InvalidArgumentException("minPrice must not be negative") }
        command.maxPrice?.let { if (it.signum() < 0) throw InvalidArgumentException("maxPrice must not be negative") }
        if (command.minPrice != null && command.maxPrice != null && command.minPrice > command.maxPrice) {
            throw InvalidArgumentException("maxPrice must not be lower than minPrice")
        }
    }
}
