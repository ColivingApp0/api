package com.coliving.api.accommodation.domain

import com.coliving.api.accommodation.domain.enums.AvailabilityState
import com.coliving.api.accommodation.domain.model.AvailabilitySlot
import com.coliving.api.shared.error.ConflictException
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AvailabilitySlotTest {

    private val unitId = UUID.randomUUID()
    private val date = LocalDate.of(2026, 10, 1)
    private val reservationId = UUID.randomUUID()

    @Test
    fun `free slot can be locked on behalf of a reservation`() {
        val slot = AvailabilitySlot.of(unitId, date)

        slot.lock(reservationId)

        assertEquals(AvailabilityState.BLOQUEADO, slot.state)
        assertEquals(reservationId, slot.reservationId)
    }

    @Test
    fun `host block uses null reservation id`() {
        val slot = AvailabilitySlot.of(unitId, date)

        slot.blockByHost()

        assertEquals(AvailabilityState.BLOQUEADO, slot.state)
        assertNull(slot.reservationId)
    }

    @Test
    fun `cannot lock an already locked day`() {
        val slot = AvailabilitySlot.of(unitId, date)
        slot.lock(reservationId)

        assertFailsWith<ConflictException> { slot.lock(UUID.randomUUID()) }
    }

    @Test
    fun `confirmed reservation occupies the day`() {
        val slot = AvailabilitySlot.of(unitId, date)
        slot.lock(reservationId)

        slot.confirm()

        assertEquals(AvailabilityState.OCUPADO, slot.state)
        assertEquals(reservationId, slot.reservationId)
    }

    @Test
    fun `release returns a locked or occupied day to available`() {
        val locked = AvailabilitySlot.of(unitId, date)
        locked.lock(reservationId)
        locked.release()
        assertEquals(AvailabilityState.DISPONIBLE, locked.state)
        assertNull(locked.reservationId)

        val occupied = AvailabilitySlot.of(unitId, date.plusDays(1), state = AvailabilityState.OCUPADO, reservationId = reservationId)
        occupied.release()
        assertEquals(AvailabilityState.DISPONIBLE, occupied.state)
    }

    @Test
    fun `cannot confirm a day that was never locked`() {
        val slot = AvailabilitySlot.of(unitId, date)
        assertFailsWith<IllegalStateException> { slot.confirm() }
    }

    @Test
    fun `cannot release a free day`() {
        val slot = AvailabilitySlot.of(unitId, date)
        assertFailsWith<IllegalStateException> { slot.release() }
    }
}