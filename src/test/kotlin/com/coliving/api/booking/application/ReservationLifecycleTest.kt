package com.coliving.api.booking.application

import com.coliving.api.booking.application.dto.CancelReservationCommand
import com.coliving.api.booking.application.dto.ConfirmReservationCommand
import com.coliving.api.booking.application.dto.CreateReservationCommand
import com.coliving.api.booking.application.dto.HostDecisionCommand
import com.coliving.api.booking.application.usecase.AcceptReservationService
import com.coliving.api.booking.application.usecase.CancelReservationService
import com.coliving.api.booking.application.usecase.ConfirmReservationService
import com.coliving.api.booking.application.usecase.CreateReservationService
import com.coliving.api.booking.application.usecase.ExpireStaleHoldsService
import com.coliving.api.booking.application.usecase.RejectReservationService
import com.coliving.api.booking.domain.enums.ReservationEventType
import com.coliving.api.booking.domain.enums.ReservationStatus
import com.coliving.api.shared.error.ForbiddenException
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Full lifecycle of a reservation with in-memory fakes: create -> host
 * decision -> guest confirm / cancel / expiry, asserting the interaction
 * contract with accommodation's availability port.
 */
class ReservationLifecycleTest {

    private val reservations = FakeReservationRepository()
    private val events = FakeReservationEventRepository()
    private val catalog = FakeUnitCatalogPort()
    private val availability = FakeUnitAvailabilityPort()
    private val verification = FakeGuestVerificationPort(verified = true)

    private val createService = CreateReservationService(
        reservations, events, catalog, availability, verification,
    )
    private val acceptService = AcceptReservationService(reservations, createService, availability, holdHours = 24)
    private val rejectService = RejectReservationService(reservations, createService)
    private val expireService = ExpireStaleHoldsService(reservations, events, availability)
    private val confirmService =
        ConfirmReservationService(reservations, createService, availability, expireService)
    private val cancelService = CancelReservationService(reservations, createService, availability)

    private val guestId = UUID.randomUUID()
    private val hostId = UUID.randomUUID()

    init {
        catalog.ownerHostId = hostId
    }

    private fun createReservation() = createService.create(
        CreateReservationCommand(
            guestId = guestId,
            unitId = FakeUnitCatalogPort.UNIT_ID,
            fromDate = LocalDate.now().plusDays(7),
            toDate = LocalDate.now().plusDays(11),
            occupants = 2,
            message = "Hola",
        ),
    )

    @Test
    fun `accept locks the range and starts the provisional hold (RF-041, RF-043)`() {
        val created = createReservation()

        val accepted = acceptService.accept(
            HostDecisionCommand(created.id, hostId, reason = null),
        )

        assertEquals(ReservationStatus.ACEPTADA, accepted.status)
        assertTrue(availability.locked.contains(created.id))
        assertTrue { accepted.holdExpiresAt != null }
        assertEquals(ReservationEventType.ACEPTADA, events.events.last().type)
    }

    @Test
    fun `only the host of the unit can decide`() {
        val created = createReservation()

        assertFailsWith<ForbiddenException> {
            acceptService.accept(HostDecisionCommand(created.id, UUID.randomUUID(), null))
        }
    }

    @Test
    fun `confirm inside the window occupies the days`() {
        val created = createReservation()
        acceptService.accept(HostDecisionCommand(created.id, hostId, null))

        val confirmed = confirmService.confirm(ConfirmReservationCommand(created.id, guestId))

        assertEquals(ReservationStatus.CONFIRMADA, confirmed.status)
        assertTrue(availability.confirmed.contains(created.id))
    }

    @Test
    fun `cancelling a confirmed reservation releases the days (RF-044)`() {
        val created = createReservation()
        acceptService.accept(HostDecisionCommand(created.id, hostId, null))
        confirmService.confirm(ConfirmReservationCommand(created.id, guestId))

        val cancelled = cancelService.cancel(
            CancelReservationCommand(created.id, guestId, reason = "No puedo viajar"),
        )

        assertEquals(ReservationStatus.CANCELADA, cancelled.status)
        assertTrue(availability.unlocked.contains(created.id))
        assertEquals(ReservationEventType.CANCELADA, events.events.last().type)
        assertEquals("No puedo viajar", events.events.last().reason)
    }

    @Test
    fun `cancellation reason is mandatory (RF-044)`() {
        val created = createReservation()

        assertFailsWith<com.coliving.api.shared.error.InvalidArgumentException> {
            cancelService.cancel(CancelReservationCommand(created.id, guestId, reason = "  "))
        }
    }

    @Test
    fun `expired holds release the inventory automatically (RF-043)`() {
        val created = createReservation()
        // Zero-hour hold: the window closes immediately after acceptance.
        val zeroHoldAccept = AcceptReservationService(reservations, createService, availability, holdHours = 0)
        zeroHoldAccept.accept(HostDecisionCommand(created.id, hostId, null))

        val expired = expireService.expireStale(Instant.now().plusSeconds(1))

        assertEquals(1, expired)
        assertEquals(ReservationStatus.EXPIRADA, reservations.store[created.id]!!.status)
        assertTrue(availability.unlocked.contains(created.id))
        assertEquals(ReservationEventType.EXPIRADA, events.events.last().type)
    }

    @Test
    fun `reject closes the request without touching the inventory`() {
        val created = createReservation()

        val rejected = rejectService.reject(
            HostDecisionCommand(created.id, hostId, reason = "No disponible"),
        )

        assertEquals(ReservationStatus.RECHAZADA, rejected.status)
        assertTrue(availability.locked.isEmpty())
        assertTrue(availability.unlocked.isEmpty())
    }
}
