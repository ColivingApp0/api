package com.coliving.api.booking.domain

import com.coliving.api.booking.domain.enums.ReservationStatus
import com.coliving.api.booking.domain.model.Reservation
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.InvalidArgumentException
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Monthly request lifecycle: no dates, no nights, acceptance occupies a place. */
class ReservationTest {

    private val now = Instant.parse("2026-10-29T14:00:00Z")

    private fun request(places: Int = 1): Reservation = Reservation.request(
        id = UUID.randomUUID(),
        unitId = UUID.randomUUID(),
        publicationId = UUID.randomUUID(),
        guestUserId = UUID.randomUUID(),
        places = places,
        message = "  Hola  ",
        monthlyPrice = BigDecimal("650000"),
        currency = "COP",
        priceUnit = "POR_PERSONA",
        now = now,
    )

    @Test
    fun `a request freezes the monthly price and starts pending`() {
        val reservation = request()

        assertEquals(ReservationStatus.SOLICITADA, reservation.status)
        assertEquals(BigDecimal("650000"), reservation.monthlyPrice)
        assertEquals("POR_PERSONA", reservation.priceUnit)
        assertEquals("Hola", reservation.message)
        assertFalse(reservation.holdsInventory)
    }

    @Test
    fun `accepting occupies the place and rejecting or cancelling free it`() {
        val accepted = request()
        accepted.accept(now.plusSeconds(60))
        assertEquals(ReservationStatus.ACEPTADA, accepted.status)
        assertTrue(accepted.holdsInventory)

        val rejected = request()
        rejected.reject(now)
        assertEquals(ReservationStatus.RECHAZADA, rejected.status)
        assertFalse(rejected.holdsInventory)

        val cancelled = request()
        cancelled.accept(now)
        cancelled.cancel(now.plusSeconds(60))
        assertEquals(ReservationStatus.CANCELADA, cancelled.status)
        assertFalse(cancelled.holdsInventory)
    }

    @Test
    fun `terminal requests cannot be accepted again`() {
        val rejected = request()
        rejected.reject(now)
        assertFailsWith<ConflictException> { rejected.accept(now) }

        val cancelled = request()
        cancelled.cancel(now)
        assertFailsWith<ConflictException> { cancelled.cancel(now) }
    }

    @Test
    fun `validates positive places and monthly price`() {
        assertFailsWith<InvalidArgumentException> { request(places = 0) }
        assertFailsWith<InvalidArgumentException> {
            Reservation.request(
                id = UUID.randomUUID(),
                unitId = UUID.randomUUID(),
                publicationId = UUID.randomUUID(),
                guestUserId = UUID.randomUUID(),
                places = 1,
                message = null,
                monthlyPrice = BigDecimal.ZERO,
                currency = "COP",
                priceUnit = "POR_HABITACION",
                now = now,
            )
        }
    }
}
