package com.coliving.api.search.application.usecase

import com.coliving.api.search.application.dto.ListingSort
import com.coliving.api.search.application.dto.ListingView
import com.coliving.api.search.application.dto.SearchListingsCommand
import com.coliving.api.search.application.port.out.ListingFacet
import com.coliving.api.search.application.port.out.PublicationCatalogPort
import com.coliving.api.shared.error.InvalidArgumentException
import java.math.BigDecimal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Monthly room search (SRS group "Búsqueda", adapted to monthly rent):
 * combinable filters over published rooms, deterministic ordering, and an
 * explicit availability state. Dates/nights are not part of this model.
 */
@Service
class SearchListingsService(
    private val publicationCatalogPort: PublicationCatalogPort,
) {

    @Transactional(readOnly = true)
    fun search(command: SearchListingsCommand): List<ListingView> {
        validate(command)
        var listings = publicationCatalogPort.findPublishedListings()

        command.q?.trim()?.takeIf { it.isNotEmpty() }?.let { query ->
            val needle = query.lowercase()
            listings = listings.filter { facet ->
                listOfNotNull(
                    facet.roomTitle,
                    facet.propertyTitle,
                    facet.cityName,
                    facet.neighborhood,
                    facet.description,
                ).any { it.lowercase().contains(needle) }
            }
        }
        command.cityId?.let { cityId -> listings = listings.filter { it.cityId == cityId } }
        command.propertyId?.let { propertyId -> listings = listings.filter { it.propertyId == propertyId } }

        if (command.minPrice != null || command.maxPrice != null || command.currency != null) {
            listings = listings.filter { facet ->
                val price = facet.monthlyPrice ?: return@filter false
                if (command.currency != null && facet.currency != command.currency) return@filter false
                val min = command.minPrice
                val max = command.maxPrice
                (min == null || price >= min) && (max == null || price <= max)
            }
        }
        command.minCapacity?.let { capacity ->
            listings = listings.filter { it.capacity >= capacity }
        }
        if (command.availableOnly) {
            listings = listings.filter { it.availablePlaces > 0 }
        }
        command.airConditioning?.let { required ->
            listings = listings.filter { it.airConditioning == required }
        }
        command.fan?.let { required -> listings = listings.filter { it.fan == required } }
        command.bathroomType?.let { value -> listings = listings.filter { it.bathroomType == value } }
        command.roomType?.let { value -> listings = listings.filter { it.roomType == value } }
        command.services?.takeIf { it.isNotEmpty() }?.let { requested ->
            listings = listings.filter { it.serviceCodes.containsAll(requested) }
        }
        command.accessibilityCode?.let { accessibility ->
            listings = listings.filter { accessibility in it.accessibilityCodes }
        }

        val sorted = when (command.sort) {
            ListingSort.PRICE_ASC -> listings.sortedWith(
                compareBy<ListingFacet> { it.monthlyPrice ?: BigDecimal.valueOf(Long.MAX_VALUE) }
                    .thenBy { it.publicationId },
            )
            ListingSort.PRICE_DESC -> listings.sortedWith(
                compareByDescending<ListingFacet> { it.monthlyPrice ?: BigDecimal.ZERO }
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
                propertyId = facet.propertyId,
                roomTitle = facet.roomTitle,
                propertyTitle = facet.propertyTitle,
                description = facet.description,
                cityId = facet.cityId,
                cityName = facet.cityName,
                neighborhood = facet.neighborhood,
                monthlyPrice = facet.monthlyPrice,
                currency = facet.currency,
                priceUnit = facet.priceUnit,
                roomType = facet.roomType,
                bathroomType = facet.bathroomType,
                airConditioning = facet.airConditioning,
                fan = facet.fan,
                capacity = facet.capacity,
                occupiedPlaces = facet.occupiedPlaces,
                availablePlaces = facet.availablePlaces,
                photoUrl = facet.photoUrl,
                propertyPhotoUrl = facet.propertyPhotoUrl,
                serviceCodes = facet.serviceCodes,
                accessibilityCodes = facet.accessibilityCodes,
                available = facet.availablePlaces > 0,
                availabilityLabel = when {
                    facet.availablePlaces <= 0 -> "Ocupada"
                    facet.capacity > 1 && facet.availablePlaces == 1 -> "Últimas plazas"
                    else -> "Disponible"
                },
            )
        }
    }

    private fun validate(command: SearchListingsCommand) {
        command.minPrice?.let {
            if (it.signum() < 0) throw InvalidArgumentException("minPrice no puede ser negativo")
        }
        command.maxPrice?.let {
            if (it.signum() < 0) throw InvalidArgumentException("maxPrice no puede ser negativo")
        }
        if (command.minPrice != null && command.maxPrice != null && command.minPrice > command.maxPrice) {
            throw InvalidArgumentException("maxPrice no puede ser menor que minPrice")
        }
        command.minCapacity?.let {
            if (it < 1) throw InvalidArgumentException("La capacidad mínima debe ser al menos 1")
        }
    }
}
