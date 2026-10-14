package com.coliving.api.search.application

import com.coliving.api.search.application.port.out.ListingFacet
import com.coliving.api.search.application.port.out.PublicationCatalogPort
import com.coliving.api.search.domain.model.Favorite
import com.coliving.api.search.domain.repository.FavoriteRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class FakePublicationCatalogPort : PublicationCatalogPort {
    val listings = mutableListOf<ListingFacet>()

    override fun findPublishedListings(): List<ListingFacet> = listings.toList()

    override fun isPublished(publicationId: UUID): Boolean =
        listings.any { it.publicationId == publicationId }

    fun add(
        publicationId: UUID = UUID.randomUUID(),
        roomTitle: String = "Habitación",
        propertyTitle: String = "Casa",
        cityName: String? = "Bogotá",
        price: BigDecimal? = BigDecimal("800000"),
        currency: String? = "COP",
        capacity: Int = 2,
        available: Int = 2,
        airConditioning: Boolean = false,
        fan: Boolean = false,
        bathroomType: String = "COMPARTIDO",
        roomType: String = "COMPARTIDA",
        updatedAt: Instant = Instant.EPOCH,
    ): ListingFacet {
        val facet = ListingFacet(
            publicationId = publicationId,
            unitId = UUID.randomUUID(),
            propertyId = UUID.randomUUID(),
            roomTitle = roomTitle,
            propertyTitle = propertyTitle,
            description = "$propertyTitle · $roomTitle",
            cityId = null,
            cityName = cityName,
            neighborhood = null,
            monthlyPrice = price,
            currency = currency,
            priceUnit = "POR_PERSONA",
            roomType = roomType,
            bathroomType = bathroomType,
            airConditioning = airConditioning,
            fan = fan,
            capacity = capacity,
            occupiedPlaces = capacity - available,
            availablePlaces = available,
            photoUrl = null,
            propertyPhotoUrl = null,
            updatedAt = updatedAt,
        )
        listings.add(facet)
        return facet
    }
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
