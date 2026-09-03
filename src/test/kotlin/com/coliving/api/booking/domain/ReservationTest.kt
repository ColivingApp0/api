package com.coliving.api.booking.domain

import com.coliving.api.booking.domain.enums.CancellationPolicyType
import com.coliving.api.booking.domain.enums.ReservationStatus
import com.coliving.api.booking.domain.model.Reservation
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.InvalidArgumentException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReservationTest {

    private val now: Instant = Instant.parse("2026-09-14T12:00:00Z")
    private val from = LocalDate.of(2026, 10, 1)
    private val to = LocalDate.of(2026, 10, 5)

    private fun newReservation(): Reservation =
        Reservation.request(
            id = UUID.randomUUID(),
            unitId = UUID.randomUUID(),
            publicationId = UUID.randomUUID(),
            guestUserId = UUID.randomUUID(),
            fromDate = from,
            toDate = to,
            occupants = 2,
            message = "Hola",
            pricePerNight = BigDecimal("80000"),
            currency = "COP",
            cancellationPolicy = CancellationPolicyType.MODERADA,
            now = now,
        )

    @Test
    fun `request freezes the economic snapshot (RN-07)`() {
        val reservation = newReservation()

        assertEquals(ReservationStatus.SOLICITADA, reservation.status)
        assertEquals(4, reservation.nights)
        assertEquals(BigDecimal("320000"), reservation.totalPrice)
        assertEquals(BigDecimal("80000"), reservation.pricePerNight)
        assertEquals(CancellationPolicyType.MODERADA, reservation.cancellationPolicy)
        assertNull(reservation.holdExpiresAt)
        assertFalse(reservation.holdsInventory)
    }

    @Test
    fun `accept locks inventory and sets the hold window (RF-043)`() {
        val reservation = newReservation()
        val deadline = now.plusSeconds(86_400)

        reservation.accept(deadline, now)

        assertEquals(ReservationStatus.ACEPTADA, reservation.status)
        assertEquals(deadline, reservation.holdExpiresAt)
        assertTrue(reservation.holdsInventory)
    }

    @Test
    fun `confirm inside the hold window moves to CONFIRMADA`() {
        val reservation = newReservation()
        reservation.accept(now.plusSeconds(3_600), now)

        reservation.confirm(now.plusSeconds(1_800))

        assertEquals(ReservationStatus.CONFIRMADA, reservation.status)
        assertTrue(reservation.holdsInventory)
    }

    @Test
    fun `confirm after the hold window is a conflict`() {
        val reservation = newReservation()
        reservation.accept(now.plusSeconds(3_600), now)

        val later = now.plusSeconds(7_200)
        assertFailsWith<ConflictException> { reservation.confirm(later) }
        assertEquals(ReservationStatus.ACEPTADA, reservation.status)
    }

    @Test
    fun `invalid transitions are rejected (RF-042)`() {
        val reservation = newReservation()
        assertFailsWith<ConflictException> { reservation.confirm(now) }
        assertFailsWith<ConflictException> { reservation.expire(now) }

        reservation.reject(now)
        assertFailsWith<ConflictException> { reservation.cancel(now) }
        assertFailsWith<ConflictException> { reservation.reject(now) }
    }

    @Test
    fun `cancel is allowed while the request is alive and terminal afterwards`() {
        val reservation = newReservation()
        reservation.cancel(now)
        assertEquals(ReservationStatus.CANCELADA, reservation.status)
    }

    @Test
    fun `expire only applies to accepted reservations`() {
        val reservation = newReservation()
        assertFailsWith<ConflictException> { reservation.expire(now) }

        reservation.accept(now.plusSeconds(3_600), now)
        reservation.expire(now.plusSeconds(7_200))
        assertEquals(ReservationStatus.EXPIRADA, reservation.status)
        assertFalse(reservation.holdsInventory)
    }

    @Test
    fun `creation validations`() {
        assertFailsWith<InvalidArgumentException> {
            Reservation.request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                from, from, 1, null, BigDecimal.TEN, "COP", null, now)
        }
        assertFailsWith<InvalidArgumentException> {
            Reservation.request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                from, to, 0, null, BigDecimal.TEN, "COP", null, now)
        }
        assertFailsWith<InvalidArgumentException> {
            Reservation.request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                from, to, 1, null, BigDecimal.ZERO, "COP", null, now)
        }
        assertFailsWith<InvalidArgumentException> {
            Reservation.request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.of(2020, 1, 1), to, 1, null, BigDecimal.TEN, "COP", null, now)
        }
    }
}
