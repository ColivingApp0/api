package com.coliving.api.search.application

import com.coliving.api.search.application.port.out.ListingFacet
import com.coliving.api.search.application.port.out.PublicationCatalogPort
import com.coliving.api.search.application.port.out.UnitAvailabilityPort
import com.coliving.api.search.domain.model.Favorite
import com.coliving.api.search.domain.repository.FavoriteRepository
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class FakePublicationCatalogPort : PublicationCatalogPort {
    val listings = mutableListOf<ListingFacet>()

    override fun findPublishedListings(): List<ListingFacet> = listings.toList()

    override fun isPublished(publicationId: UUID): Boolean =
        listings.any { it.publicationId == publicationId }

    fun add(
        publicationId: UUID = UUID.randomUUID(),
        title: String = "Listing",
        cityId: UUID? = null,
        price: BigDecimal? = BigDecimal("100000"),
        currency: String? = "COP",
        updatedAt: Instant = Instant.EPOCH,
    ): ListingFacet {
        val facet = ListingFacet(
            publicationId = publicationId,
            unitId = UUID.randomUUID(),
            title = title,
            cityId = cityId,
            pricePerNight = price,
            currency = currency,
            updatedAt = updatedAt,
        )
        listings.add(facet)
        return facet
    }
}

class FakeSearchAvailabilityPort : UnitAvailabilityPort {
    val unavailable = mutableSetOf<UUID>()

    override fun isRangeAvailable(unitId: UUID, from: LocalDate, to: LocalDate): Boolean =
        unitId !in unavailable
}

class FakeFavoriteRepository : FavoriteRepository {
    val store = mutableMapOf<UUID, Favorite>()

    override fun findByUser(userId: UUID): List<Favorite> =
        store.values.filter { it.userId == userId }.sortedByDescending { it.createdAt }

    override fun findIdsByUser(userId: UUID): Set<UUID> =
        store.values.filter { it.userId == userId }.map { it.publicationId }.toSet()

    override fun existsByUserAndPublication(userId: UUID, publicationId: UUID): Boolean =
        store.values.any { it.userId == userId && it.publicationId == publicationId }

    override fun save(favorite: Favorite) {
        store[favorite.id] = favorite
    }

    override fun delete(userId: UUID, publicationId: UUID) {
        store.entries.removeIf { it.value.userId == userId && it.value.publicationId == publicationId }
    }
}
