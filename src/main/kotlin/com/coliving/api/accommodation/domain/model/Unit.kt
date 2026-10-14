package com.coliving.api.accommodation.domain.model

import com.coliving.api.accommodation.domain.enums.BathroomType
import com.coliving.api.accommodation.domain.enums.Currency
import com.coliving.api.accommodation.domain.enums.PriceUnit
import com.coliving.api.accommodation.domain.enums.RoomType
import com.coliving.api.shared.error.InvalidArgumentException
import java.math.BigDecimal
import java.util.UUID

/**
 * Rentable room inside a property (the previous `Unit` granularity). It carries
 * its own monthly rent, capacity, characteristics and photographs.
 *
 * Occupancy is NOT stored here: it is derived from accepted reservations so it
 * can never drift from the real records (see the occupancy query port).
 * Effective availability = published/commercial availability AND free places.
 */
class Unit(
    val id: UUID,
    val propertyId: UUID,
    name: String,
    description: String?,
    maxGuests: Int,
    monthlyPrice: BigDecimal?,
    currency: Currency,
    priceUnit: PriceUnit,
    roomType: RoomType,
    bathroomType: BathroomType,
    airConditioning: Boolean,
    fan: Boolean,
    commonAreas: List<String>,
    typeCode: UUID? = null,
    accessibilityCodes: Set<UUID> = emptySet(),
    sortOrder: Int = 0,
) {

    var name: String = name
        private set

    var description: String? = description
        private set

    /** Maximum number of people/places the room can host. */
    var maxGuests: Int = maxGuests
        private set

    var monthlyPrice: BigDecimal? = monthlyPrice
        private set

    var currency: Currency = currency
        private set

    var priceUnit: PriceUnit = priceUnit
        private set

    var roomType: RoomType = roomType
        private set

    var bathroomType: BathroomType = bathroomType
        private set

    var airConditioning: Boolean = airConditioning
        private set

    var fan: Boolean = fan
        private set

    var commonAreas: List<String> = commonAreas
        private set

    /**
     * Room type of the unit, a TIPO_HABITACION catalog entry reference
     * (RF-031, RF-083). Plain UUID, no FK, like every cross-context reference.
     */
    var typeCode: UUID? = typeCode
        private set

    /** Accessibility features of the unit (ACCESIBILIDAD catalog entries, RF-031). */
    var accessibilityCodes: Set<UUID> = accessibilityCodes
        private set

    var sortOrder: Int = sortOrder
        private set

    fun update(
        name: String,
        description: String?,
        maxGuests: Int,
        monthlyPrice: BigDecimal?,
        currency: Currency,
        priceUnit: PriceUnit,
        roomType: RoomType,
        bathroomType: BathroomType,
        airConditioning: Boolean,
        fan: Boolean,
        commonAreas: List<String>,
        typeCode: UUID? = null,
        accessibilityCodes: Set<UUID>? = null,
        sortOrder: Int? = null,
        occupiedPlaces: Int = 0,
    ) {
        if (maxGuests <= 0) throw InvalidArgumentException("La capacidad debe ser al menos 1")
        if (occupiedPlaces > maxGuests) {
            throw InvalidArgumentException(
                "La capacidad no puede ser menor que las plazas ocupadas ($occupiedPlaces)",
            )
        }
        if (monthlyPrice != null && monthlyPrice.signum() <= 0) {
            throw InvalidArgumentException("El precio mensual debe ser positivo")
        }
        if (roomType == RoomType.INDIVIDUAL && maxGuests > 1) {
            throw InvalidArgumentException("Una habitación individual no puede tener más de una plaza")
        }
        this.name = name.trim()
        if (this.name.isBlank()) throw InvalidArgumentException("El nombre es obligatorio")
        this.description = description?.trim()?.takeIf { it.isNotBlank() }
        this.maxGuests = maxGuests
        this.monthlyPrice = monthlyPrice
        this.currency = currency
        this.priceUnit = if (roomType == RoomType.INDIVIDUAL) PriceUnit.POR_HABITACION else priceUnit
        this.roomType = roomType
        this.bathroomType = bathroomType
        this.airConditioning = airConditioning
        this.fan = fan
        this.commonAreas = commonAreas.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        // null keeps the current reference: the update endpoint may only send
        // the fields it wants to change.
        if (typeCode != null) this.typeCode = typeCode
        if (accessibilityCodes != null) this.accessibilityCodes = accessibilityCodes
        if (sortOrder != null) this.sortOrder = sortOrder
    }

    /** Free places for the current occupancy, never negative. */
    fun availablePlaces(occupiedPlaces: Int): Int = (maxGuests - occupiedPlaces).coerceAtLeast(0)

    /** Dedicated mutation for the pricing endpoint, without resending the room. */
    fun changeMonthlyPrice(
        monthlyPrice: BigDecimal,
        currency: Currency,
        priceUnit: PriceUnit = this.priceUnit,
    ) {
        if (monthlyPrice.signum() <= 0) {
            throw InvalidArgumentException("El precio mensual debe ser positivo")
        }
        this.monthlyPrice = monthlyPrice
        this.currency = currency
        this.priceUnit = if (roomType == RoomType.INDIVIDUAL) PriceUnit.POR_HABITACION else priceUnit
    }

    companion object {
        fun create(
            id: UUID,
            propertyId: UUID,
            name: String,
            description: String? = null,
            maxGuests: Int,
            monthlyPrice: BigDecimal? = null,
            currency: Currency = Currency.COP,
            priceUnit: PriceUnit = PriceUnit.POR_HABITACION,
            roomType: RoomType = RoomType.INDIVIDUAL,
            bathroomType: BathroomType = BathroomType.COMPARTIDO,
            airConditioning: Boolean = false,
            fan: Boolean = false,
            commonAreas: List<String> = emptyList(),
            typeCode: UUID? = null,
            accessibilityCodes: Set<UUID> = emptySet(),
            sortOrder: Int = 0,
        ): Unit {
            val unit = Unit(
                id = id,
                propertyId = propertyId,
                name = name,
                description = description,
                maxGuests = maxGuests,
                monthlyPrice = monthlyPrice,
                currency = currency,
                priceUnit = priceUnit,
                roomType = roomType,
                bathroomType = bathroomType,
                airConditioning = airConditioning,
                fan = fan,
                commonAreas = commonAreas,
                typeCode = typeCode,
                accessibilityCodes = accessibilityCodes,
                sortOrder = sortOrder,
            )
            unit.update(
                name = name,
                description = description,
                maxGuests = maxGuests,
                monthlyPrice = monthlyPrice,
                currency = currency,
                priceUnit = priceUnit,
                roomType = roomType,
                bathroomType = bathroomType,
                airConditioning = airConditioning,
                fan = fan,
                commonAreas = commonAreas,
                typeCode = typeCode,
                accessibilityCodes = accessibilityCodes,
                sortOrder = sortOrder,
            )
            return unit
        }
    }
}
