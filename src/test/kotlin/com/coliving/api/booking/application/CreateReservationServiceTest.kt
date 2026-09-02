package com.coliving.api.booking.application

import com.coliving.api.booking.application.dto.CreateReservationCommand
import com.coliving.api.booking.application.port.out.BookableUnit
import com.coliving.api.booking.application.usecase.CreateReservationService
import com.coliving.api.booking.domain.enums.ReservationEventType
import com.coliving.api.booking.domain.enums.ReservationStatus
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.NotFoundException
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CreateReservationServiceTest {

    private val reservations = FakeReservationRepository()
    private val events = FakeReservationEventRepository()
    private val catalog = FakeUnitCatalogPort()
    private val availability = FakeUnitAvailabilityPort()
    private val verification = FakeGuestVerificationPort(verified = true)

    private val service = CreateReservationService(
        reservations, events, catalog, availability, verification,
    )

    private val guestId = UUID.randomUUID()
    private val unitId = FakeUnitCatalogPort.UNIT_ID
    private val from = LocalDate.now().plusDays(7)
    private val to = LocalDate.now().plusDays(11)

    private fun command(
        occupants: Int = 2,
        unit: UUID = unitId,
        fromDate: LocalDate = from,
        toDate: LocalDate = to,
    ) = CreateReservationCommand(guestId, unit, fromDate, toDate, occupants, "Hola")

    @Test
    fun `creates a request with the frozen economic snapshot (RN-07)`() {
        val view = service.create(command())

        assertEquals(ReservationStatus.SOLICITADA, view.status)
        assertEquals(BigDecimal("80000"), view.pricePerNight)
        assertEquals(BigDecimal("320000"), view.totalPrice)
        assertEquals("COP", view.currency)
        assertEquals(2, view.occupants)
        assertEquals(1, events.events.size)
        assertEquals(ReservationEventType.CREADA, events.events.single().type)
    }

    @Test
    fun `rejects a unit without a PUBLICADA publication (RN-03)`() {
        catalog.bookable = catalog.bookable?.copy(status = "BORRADOR")

        assertFailsWith<ConflictException> { service.create(command()) }
    }

    @Test
    fun `rejects an unknown unit`() {
        assertFailsWith<NotFoundException> { service.create(command(unit = UUID.randomUUID())) }
    }

    @Test
    fun `rejects occupants above unit capacity (RF-040)`() {
        assertFailsWith<ConflictException> { service.create(command(occupants = 5)) }
    }

    @Test
    fun `rejects guests below the minimum verification level (RF-040)`() {
        verification.setVerified(false)

        assertFailsWith<ForbiddenException> { service.create(command()) }
    }

    @Test
    fun `rejects unavailable dates`() {
        availability.available = false

        assertFailsWith<ConflictException> { service.create(command()) }
    }

    @Test
    fun `maps an unknown policy to null instead of failing the request`() {
        catalog.bookable = catalog.bookable?.copy(cancellationPolicy = "DESARCONOCIDA")

        val view = service.create(command())
        assertTrue { view.cancellationPolicy == null }
    }
}
