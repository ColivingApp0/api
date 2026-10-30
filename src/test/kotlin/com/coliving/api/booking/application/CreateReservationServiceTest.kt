package com.coliving.api.booking.application

import com.coliving.api.accommodation.domain.enums.Currency
import com.coliving.api.accommodation.domain.enums.PriceUnit
import com.coliving.api.booking.application.dto.CreateReservationCommand
import com.coliving.api.booking.application.port.out.BookableUnit
import com.coliving.api.booking.application.port.out.GuestVerificationPort
import com.coliving.api.booking.application.port.out.UnitCatalogPort
import com.coliving.api.booking.application.usecase.CreateReservationService
import com.coliving.api.booking.domain.enums.ReservationStatus
import com.coliving.api.booking.domain.model.Reservation
import com.coliving.api.booking.domain.model.ReservationEvent
import com.coliving.api.booking.domain.repository.ReservationEventRepository
import com.coliving.api.booking.domain.repository.ReservationRepository
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private class InMemoryReservations : ReservationRepository {
    val items = mutableListOf<Reservation>()

    override fun findById(id: UUID): Reservation? = items.firstOrNull { it.id == id }
    override fun save(reservation: Reservation) {
        items.removeIf { it.id == reservation.id }
        items.add(reservation)
    }

    override fun findByGuest(guestUserId: UUID): List<Reservation> = items.filter { it.guestUserId == guestUserId }
    override fun findByUnitIds(unitIds: List<UUID>): List<Reservation> = items.filter { it.unitId in unitIds }
    override fun occupiedPlacesOf(unitIds: Collection<UUID>): Map<UUID, Int> =
        items.filter { it.unitId in unitIds && it.holdsInventory }
            .groupBy { it.unitId }
            .mapValues { (_, value) -> value.sumOf { it.places } }

    override fun residentIdsOf(unitId: UUID): List<UUID> =
        items.filter { it.unitId == unitId && it.holdsInventory }.map { it.guestUserId }.distinct()

    override fun hasActiveRequest(guestUserId: UUID, unitId: UUID): Boolean =
        items.any {
            it.guestUserId == guestUserId && it.unitId == unitId &&
                it.status in setOf(
                    ReservationStatus.SOLICITADA,
                    ReservationStatus.INFORMACION_SOLICITADA,
                    ReservationStatus.ACEPTADA,
                    ReservationStatus.CONFIRMADA,
                )
        }
}

private class InMemoryEvents : ReservationEventRepository {
    val items = mutableListOf<ReservationEvent>()
    override fun findByReservation(reservationId: UUID): List<ReservationEvent> =
        items.filter { it.reservationId == reservationId }
    override fun save(event: ReservationEvent) {
        items.add(event)
    }
}

private class FakeCatalog : UnitCatalogPort {
    var room: BookableUnit? = null
    var currentOccupyByOther = 0

    override fun findBookableUnit(unitId: UUID): BookableUnit? = room?.takeIf { it.unitId == unitId }
    override fun lockBookableUnit(unitId: UUID): BookableUnit? = findBookableUnit(unitId)
    override fun unitIdsOfHost(hostId: UUID): List<UUID> = listOfNotNull(room?.unitId)
    override fun isHostOfUnit(unitId: UUID, hostId: UUID): Boolean = room?.hostId == hostId
    override fun hostOfUnit(unitId: UUID): UUID? = room?.hostId
}

private class FakeVerification : GuestVerificationPort {
    var allowed = true
    override fun hasMinimumVerification(guestUserId: UUID): Boolean = allowed
}

class CreateReservationServiceTest {

    private val reservations = InMemoryReservations()
    private val events = InMemoryEvents()
    private val catalog = FakeCatalog()
    private val verification = FakeVerification()
    private val service = CreateReservationService(reservations, events, catalog, verification)

    private val hostId = UUID.randomUUID()
    private val guestId = UUID.randomUUID()
    private val propertyId = UUID.randomUUID()
    private val unitId = UUID.randomUUID()

    private fun room(
        status: String = "PUBLICADA",
        capacity: Int = 2,
        monthlyPrice: BigDecimal? = BigDecimal("650000"),
    ): BookableUnit = BookableUnit(
        unitId = unitId,
        propertyId = propertyId,
        publicationId = UUID.randomUUID(),
        status = status,
        unitName = "Habitación compartida",
        propertyTitle = "Casa norte",
        cityName = "Bogotá",
        neighborhood = "Chapinero",
        photoUrl = "/images/rooms/room-1.jpg",
        capacity = capacity,
        monthlyPrice = monthlyPrice,
        currency = Currency.COP,
        priceUnit = PriceUnit.POR_PERSONA,
        hostId = hostId,
    ).also { catalog.room = it }

    @Test
    fun `creates a pending monthly request and freezes the price`() {
        room()
        val view = service.create(CreateReservationCommand(guestId, unitId, 1, "Hola"))

        assertEquals(ReservationStatus.SOLICITADA, view.status)
        assertEquals(BigDecimal("650000"), view.monthlyPrice)
        assertEquals("POR_PERSONA", view.priceUnit)
        assertEquals("Habitación compartida", view.unitName)
        assertEquals(1, reservations.items.size)
        assertEquals(1, events.items.size)
    }

    @Test
    fun `does not occupy a place while the request is pending`() {
        room()
        service.create(CreateReservationCommand(guestId, unitId, 1, null))
        assertEquals(emptyMap(), reservations.occupiedPlacesOf(listOf(unitId)))
    }

    @Test
    fun `rejects a paused room, a room without price and a full room`() {
        room(status = "PAUSADA")
        assertFailsWith<ConflictException> {
            service.create(CreateReservationCommand(guestId, unitId, 1, null))
        }

        room(monthlyPrice = null)
        assertFailsWith<ConflictException> {
            service.create(CreateReservationCommand(guestId, unitId, 1, null))
        }

        room(capacity = 1)
        reservations.save(
            Reservation.request(
                UUID.randomUUID(), unitId, UUID.randomUUID(), UUID.randomUUID(), 1, null,
                BigDecimal("650000"), "COP", "POR_PERSONA", java.time.Instant.now(),
            ).also { it.accept(java.time.Instant.now()) },
        )
        assertFailsWith<ConflictException> {
            service.create(CreateReservationCommand(guestId, unitId, 1, null))
        }
    }

    @Test
    fun `rejects a second active request from the same guest for the same room`() {
        room()
        service.create(CreateReservationCommand(guestId, unitId, 1, null))
        assertFailsWith<ConflictException> {
            service.create(CreateReservationCommand(guestId, unitId, 1, null))
        }
    }

    @Test
    fun `rejects unverified guests`() {
        room()
        verification.allowed = false
        assertFailsWith<ForbiddenException> {
            service.create(CreateReservationCommand(guestId, unitId, 1, null))
        }
        assertTrue(reservations.items.isEmpty())
    }
}
