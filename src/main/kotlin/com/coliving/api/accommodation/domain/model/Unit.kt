package com.coliving.api.accommodation.domain.model

import com.coliving.api.shared.error.InvalidArgumentException
import java.util.UUID

/**
 * Unit inside a property; the reservation-granularity entity (availability slots
 * and (future) bookings target a unit).
 */
class Unit(
    val id: UUID,
    val propertyId: UUID,
    name: String,
    maxGuests: Int,
    bedrooms: Int,
    beds: Int,
    bathrooms: Int,
) {

    var name: String = name
        private set

    var maxGuests: Int = maxGuests
        private set

    var bedrooms: Int = bedrooms
        private set

    var beds: Int = beds
        private set

    var bathrooms: Int = bathrooms
        private set

    fun update(
        name: String,
        maxGuests: Int,
        bedrooms: Int,
        beds: Int,
        bathrooms: Int,
    ) {
        if (maxGuests <= 0) throw InvalidArgumentException("maxGuests must be positive")
        if (bedrooms < 0) throw InvalidArgumentException("bedrooms must not be negative")
        if (beds < 0) throw InvalidArgumentException("beds must not be negative")
        if (bathrooms < 0) throw InvalidArgumentException("bathrooms must not be negative")
        this.name = name.trim()
        this.maxGuests = maxGuests
        this.bedrooms = bedrooms
        this.beds = beds
        this.bathrooms = bathrooms
    }

    companion object {
        fun create(
            id: UUID,
            propertyId: UUID,
            name: String,
            maxGuests: Int,
            bedrooms: Int,
            beds: Int,
            bathrooms: Int,
        ): Unit {
            val unit = Unit(
                id = id,
                propertyId = propertyId,
                name = name,
                maxGuests = maxGuests,
                bedrooms = bedrooms,
                beds = beds,
                bathrooms = bathrooms,
            )
            unit.update(name, maxGuests, bedrooms, beds, bathrooms)
            if (unit.name.isBlank()) {
                throw InvalidArgumentException("name is required")
            }
            return unit
        }
    }
}