package com.coliving.api.accommodation.application

import com.coliving.api.accommodation.application.dto.BlockDatesCommand
import com.coliving.api.accommodation.application.dto.UnblockDatesCommand
import com.coliving.api.accommodation.application.usecase.AvailabilityLockService
import com.coliving.api.accommodation.domain.enums.AvailabilityState
import com.coliving.api.accommodation.domain.model.Property
import com.coliving.api.accommodation.domain.model.Unit
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.InvalidArgumentException
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AvailabilityLockServiceTest {

    private val slots = FakeAvailabilitySlotRepository()
    private val units = FakeUnitRepository()
    private val properties = FakePropertyRepository()

    private val service = AvailabilityLockService(slots, units, properties)

    private val hostId = UUID.randomUUID()
    private val unitId = UUID.randomUUID()

    private val property = Property.create(UUID.randomUUID(), hostId, "Casa", null, null, null, emptyList())
    private val unit = Unit.create(unitId, property.id, "Habitación", 2, 1, 1, 1)

    private val from = LocalDate.of(2026, 10, 1)
    private val to = LocalDate.of(2026, 10, 5)

    @Test
    fun `lockRange locks every night of the stay`() {
        seedUnit()

        service.lockRange(unitId, from, to, reservationId())

        val nights = slots.findRangeReadOnly(unitId, from, to)
        assertEquals(4, nights.size)
        nights.values.forEach { assertEquals(AvailabilityState.BLOQUEADO, it.state) }
    }

    @Test
    fun `lockRange fails if any night is already taken`() {
        seedUnit()
        service.blockDates(BlockDatesCommand(unitId, hostId, from, from.plusDays(1)))

        assertFailsWith<ConflictException> {
            service.lockRange(unitId, from, to, reservationId())
        }
    }

    @Test
    fun `unlockRange releases only the caller's reservation`() {
        seedUnit()
        val mine = reservationId()
        // Reservations cannot overlap a host block: lock [from, from+2),
        // then the host blocks from+2 separately.
        service.lockRange(unitId, from, from.plusDays(2), mine)
        service.blockDates(BlockDatesCommand(unitId, hostId, from.plusDays(2), from.plusDays(3)))

        service.unlockRange(unitId, from, to, mine)

        val nights = slots.findRangeReadOnly(unitId, from, to)
        assertEquals(AvailabilityState.DISPONIBLE, nights.getValue(from).state)
        assertEquals(AvailabilityState.DISPONIBLE, nights.getValue(from.plusDays(1)).state)
        // The host block is untouched (other party's hold).
        assertEquals(AvailabilityState.BLOQUEADO, nights.getValue(from.plusDays(2)).state)
        assertEquals(AvailabilityState.DISPONIBLE, nights.getValue(from.plusDays(3)).state)
    }

    @Test
    fun `isRangeAvailable reflects implicit free days and taken days`() {
        seedUnit()
        assertTrue(service.isRangeAvailable(unitId, from, to))

        service.blockDates(BlockDatesCommand(unitId, hostId, from, from.plusDays(1)))
        assertFalse(service.isRangeAvailable(unitId, from, to))
    }

    @Test
    fun `host block and unblock never touch reservation-held days`() {
        seedUnit()
        val reservation = reservationId()
        service.lockRange(unitId, from, from.plusDays(2), reservation)

        assertFailsWith<ConflictException> {
            service.blockDates(BlockDatesCommand(unitId, hostId, from, to))
        }

        // Unblock only the free days (host never blocked anything held by a reservation).
        service.unblockDates(UnblockDatesCommand(unitId, hostId, from, to))
        val nights = slots.findRangeReadOnly(unitId, from, to)
        assertEquals(AvailabilityState.BLOQUEADO, nights.getValue(from).state)
        assertEquals(reservation, nights.getValue(from).reservationId)
    }

    @Test
    fun `non-owner cannot block dates`() {
        seedUnit()
        assertFailsWith<ForbiddenException> {
            service.blockDates(BlockDatesCommand(unitId, UUID.randomUUID(), from, to))
        }
    }

    @Test
    fun `invalid ranges are rejected`() {
        seedUnit()
        assertFailsWith<InvalidArgumentException> {
            service.lockRange(unitId, to, from, reservationId())
        }
        assertFailsWith<InvalidArgumentException> {
            service.lockRange(unitId, from, from, reservationId())
        }
    }

    @Test
    fun `getAvailability lists every night with state and reserved flag`() {
        seedUnit()
        service.lockRange(unitId, from, from.plusDays(2), reservationId())

        val days = service.getAvailability(unitId, from, to)
        assertEquals(4, days.size)
        assertEquals(AvailabilityState.BLOQUEADO, days[0].state)
        assertTrue(days[0].reserved)
        assertEquals(AvailabilityState.DISPONIBLE, days[2].state)
        assertFalse(days[2].reserved)
    }

    private fun seedUnit() {
        properties.save(property)
        units.save(unit)
    }

    private fun reservationId(): UUID = UUID.randomUUID()
}