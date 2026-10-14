package com.coliving.api.accommodation.domain.model

import java.util.UUID

/**
 * Property aggregate (Propiedad), owned by a host. It holds the shared
 * information of the building; each rentable room is a [Unit] of this property.
 * [cityId] is a plain reference to the city catalog (no FK); [cityName] and
 * [neighborhood] are the human-readable location shown to guests.
 */
class Property(
    val id: UUID,
    val hostId: UUID,
    title: String?,
    description: String?,
    cityId: UUID?,
    cityName: String?,
    neighborhood: String?,
    address: String?,
    amenities: List<String>,
    archived: Boolean,
) {

    var title: String? = title
        private set

    var description: String? = description
        private set

    var cityId: UUID? = cityId
        private set

    var cityName: String? = cityName
        private set

    var neighborhood: String? = neighborhood
        private set

    var address: String? = address
        private set

    var amenities: List<String> = amenities
        private set

    var archived: Boolean = archived
        private set

    fun update(
        title: String?,
        description: String?,
        cityId: UUID?,
        cityName: String?,
        neighborhood: String?,
        address: String?,
        amenities: List<String>,
    ) {
        this.title = normalize(title)
        this.description = normalize(description)
        this.cityId = cityId
        this.cityName = normalize(cityName)
        this.neighborhood = normalize(neighborhood)
        this.address = normalize(address)
        this.amenities = amenities.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }

    fun archive() {
        archived = true
    }

    private fun normalize(value: String?): String? =
        value?.trim()?.takeIf { it.isNotBlank() }

    companion object {
        fun create(
            id: UUID,
            hostId: UUID,
            title: String,
            description: String?,
            cityId: UUID?,
            cityName: String?,
            neighborhood: String?,
            address: String?,
            amenities: List<String>,
        ): Property = Property(
            id = id,
            hostId = hostId,
            title = title.trim(),
            description = description?.trim()?.takeIf { it.isNotBlank() },
            cityId = cityId,
            cityName = cityName?.trim()?.takeIf { it.isNotBlank() },
            neighborhood = neighborhood?.trim()?.takeIf { it.isNotBlank() },
            address = address?.trim()?.takeIf { it.isNotBlank() },
            amenities = amenities.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
            archived = false,
        )
    }
}
