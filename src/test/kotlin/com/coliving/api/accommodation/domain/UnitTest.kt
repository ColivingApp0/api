package com.coliving.api.accommodation.domain

import com.coliving.api.accommodation.domain.enums.BathroomType
import com.coliving.api.accommodation.domain.enums.Currency
import com.coliving.api.accommodation.domain.enums.PriceUnit
import com.coliving.api.accommodation.domain.enums.RoomType
import com.coliving.api.accommodation.domain.model.Unit
import com.coliving.api.shared.error.InvalidArgumentException
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Monthly room invariants: capacity, occupancy, price and characteristics. */
class UnitTest {

    private fun room(
        maxGuests: Int = 2,
        roomType: RoomType = RoomType.COMPARTIDA,
        monthlyPrice: BigDecimal? = BigDecimal("800000"),
    ): Unit = Unit.create(
        id = UUID.randomUUID(),
        propertyId = UUID.randomUUID(),
        name = "Habitación compartida",
        maxGuests = maxGuests,
        monthlyPrice = monthlyPrice,
        roomType = roomType,
        bathroomType = BathroomType.PRIVADO,
        airConditioning = true,
        fan = true,
        commonAreas = listOf("Cocina", "Sala"),
    )

    @Test
    fun `available places are capacity minus current occupancy`() {
        val unit = room(maxGuests = 2)
        assertEquals(2, unit.availablePlaces(0))
        assertEquals(1, unit.availablePlaces(1))
        assertEquals(0, unit.availablePlaces(2))
        // Defensive: an inconsistent occupancy never yields negative places.
        assertEquals(0, unit.availablePlaces(3))
    }

    @Test
    fun `capacity cannot drop below the current occupancy`() {
        val unit = room(maxGuests = 2)
        assertFailsWith<InvalidArgumentException> {
            unit.update(
                name = unit.name,
                description = null,
                maxGuests = 1,
                monthlyPrice = unit.monthlyPrice,
                currency = Currency.COP,
                priceUnit = PriceUnit.POR_PERSONA,
                roomType = RoomType.COMPARTIDA,
                bathroomType = BathroomType.PRIVADO,
                airConditioning = true,
                fan = true,
                commonAreas = emptyList(),
                occupiedPlaces = 2,
            )
        }
    }

    @Test
    fun `an individual room cannot host more than one place`() {
        assertFailsWith<InvalidArgumentException> {
            room(maxGuests = 2, roomType = RoomType.INDIVIDUAL)
        }
    }

    @Test
    fun `monthly price is optional until the host configures it, but never negative`() {
        assertNull(room(monthlyPrice = null).monthlyPrice)
        assertFailsWith<InvalidArgumentException> {
            room(monthlyPrice = BigDecimal("-1"))
        }
        val unit = room(monthlyPrice = null)
        unit.changeMonthlyPrice(BigDecimal("650000"), Currency.COP, PriceUnit.POR_PERSONA)
        assertEquals(BigDecimal("650000"), unit.monthlyPrice)
    }

    @Test
    fun `individual rooms always bill per room`() {
        val unit = room(maxGuests = 1, roomType = RoomType.INDIVIDUAL)
        unit.changeMonthlyPrice(BigDecimal("900000"), Currency.COP, PriceUnit.POR_PERSONA)
        assertEquals(PriceUnit.POR_HABITACION, unit.priceUnit)
    }

    @Test
    fun `room characteristics belong to the room, not the property`() {
        val one = room()
        val other = Unit.create(
            id = UUID.randomUUID(),
            propertyId = one.propertyId,
            name = "Habitación con ventilador",
            maxGuests = 1,
            roomType = RoomType.INDIVIDUAL,
            bathroomType = BathroomType.COMPARTIDO,
            airConditioning = false,
            fan = true,
            commonAreas = emptyList(),
        )
        assertEquals(true, one.airConditioning)
        assertEquals(false, other.airConditioning)
        assertEquals(BathroomType.PRIVADO, one.bathroomType)
        assertEquals(BathroomType.COMPARTIDO, other.bathroomType)
    }
}
