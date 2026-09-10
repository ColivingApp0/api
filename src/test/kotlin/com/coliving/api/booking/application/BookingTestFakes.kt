package com.coliving.api.booking.application

import com.coliving.api.booking.application.port.out.BookableUnit
import com.coliving.api.booking.application.port.out.GuestVerificationPort
import com.coliving.api.booking.application.port.out.UnitAvailabilityPort
import com.coliving.api.booking.application.port.out.UnitCatalogPort
import com.coliving.api.booking.domain.model.Reservation
import com.coliving.api.booking.domain.model.ReservationEvent
import com.coliving.api.booking.domain.repository.ReservationEventRepository
import com.coliving.api.booking.domain.repository.ReservationRepository
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class FakeReservationRepository : ReservationRepository {
    val store = mutableMapOf<UUID, Reservation>()

    override fun findById(id: UUID): Reservation? = store[id]

    override fun save(reservation: Reservation) {
        store[reservation.id] = reservation
    }

    override fun findByGuest(guestUserId: UUID): List<Reservation> =
        store.values.filter { it.guestUserId == guestUserId }.sortedByDescending { it.createdAt }

    override fun findByUnitIds(unitIds: List<UUID>): List<Reservation> =
        store.values.filter { it.unitId in unitIds }.sortedByDescending { it.createdAt }

    override fun findExpiredHolds(now: Instant): List<Reservation> =
        store.values.filter { it.status == com.coliving.api.booking.domain.enums.ReservationStatus.ACEPTADA && it.holdExpiresAt != null && it.holdExpiresAt!!.isBefore(now) }
}

class FakeReservationEventRepository : ReservationEventRepository {
    val events = mutableListOf<ReservationEvent>()

    override fun findByReservation(reservationId: UUID): List<ReservationEvent> =
        events.filter { it.reservationId == reservationId }.sortedBy { it.occurredAt }

    override fun save(event: ReservationEvent) {
        events.add(event)
    }
}

class FakeUnitCatalogPort : UnitCatalogPort {
    var bookable: BookableUnit? = BookableUnit(
        unitId = UNIT_ID,
        publicationId = UUID.randomUUID(),
        status = "PUBLICADA",
        maxGuests = 2,
        basePricePerNight = BigDecimal("80000"),
        currency = "COP",
        cancellationPolicy = "MODERADA",
    )
    var hostUnits: List<UUID> = listOf(UNIT_ID)
    var ownerHostId: UUID = UUID.randomUUID()

    override fun findBookableUnit(unitId: UUID): BookableUnit? =
        bookable?.takeIf { it.unitId == unitId }

    override fun unitIdsOfHost(hostId: UUID): List<UUID> =
        if (hostId == ownerHostId) hostUnits else emptyList()

    override fun isHostOfUnit(unitId: UUID, hostId: UUID): Boolean =
        unitId in hostUnits && hostId == ownerHostId

    override fun hostOfUnit(unitId: UUID): UUID? =
        if (unitId in hostUnits) ownerHostId else null

    companion object {
        val UNIT_ID: UUID = UUID.randomUUID()
    }
}

/**
 * In-memory availability: locks are (unitId, from, to) ranges; overlap makes a
 * range unavailable, release removes them. Call lists let tests assert the
 * exact interaction contract with accommodation.
 */
class FakeUnitAvailabilityPort : UnitAvailabilityPort {
    val locks = mutableMapOf<UUID, MutableList<Triple<LocalDate, LocalDate, UUID>>>()
    val locked = mutableListOf<UUID>()
    val unlocked = mutableListOf<UUID>()
    val confirmed = mutableListOf<UUID>()
    var available = true

    override fun isRangeAvailable(unitId: UUID, from: LocalDate, to: LocalDate): Boolean {
        if (!available) return false
        val held = locks[unitId].orEmpty()
        return held.none { (f, t, _) -> from < t && f < to }
    }

    override fun lockRange(unitId: UUID, from: LocalDate, to: LocalDate, reservationId: UUID) {
        locks.getOrPut(unitId) { mutableListOf() }.add(Triple(from, to, reservationId))
        locked.add(reservationId)
    }

    override fun unlockRange(unitId: UUID, from: LocalDate, to: LocalDate, reservationId: UUID) {
        locks[unitId]?.removeAll { it.third == reservationId }
        unlocked.add(reservationId)
    }

    override fun confirmRange(unitId: UUID, from: LocalDate, to: LocalDate, reservationId: UUID) {
        confirmed.add(reservationId)
    }
}

class FakeGuestVerificationPort(
    private var verified: Boolean = true,
) : GuestVerificationPort {

    override fun hasMinimumVerification(guestUserId: UUID): Boolean = verified

    fun setVerified(value: Boolean) {
        verified = value
    }
}
