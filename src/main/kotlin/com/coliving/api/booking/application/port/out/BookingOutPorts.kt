package com.coliving.api.booking.application.port.out

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * Availability contract the booking context consumes from `accommodation`.
 * Implemented by an adapter delegating to accommodation's
 * ReservationAvailabilityPort (AvailabilityLockService) — the single
 * enforcement point of the no-double-booking invariant.
 */
interface UnitAvailabilityPort {

    fun isRangeAvailable(unitId: UUID, from: LocalDate, to: LocalDate): Boolean

    /** Atomically locks every day in [from, to) on behalf of [reservationId]. */
    fun lockRange(unitId: UUID, from: LocalDate, to: LocalDate, reservationId: UUID)

    /** Releases the days held by [reservationId] back to available. */
    fun unlockRange(unitId: UUID, from: LocalDate, to: LocalDate, reservationId: UUID)

    /** Held days (BLOQUEADO) become OCUPADO when the reservation is confirmed. */
    fun confirmRange(unitId: UUID, from: LocalDate, to: LocalDate, reservationId: UUID)
}

/**
 * Catalog snapshot of a unit, read from `accommodation`. Non-null only when
 * the unit and its publication exist; [BookableUnit.status] must be PUBLICADA
 * for the unit to admit requests (RN-03). Price/policy values feed the
 * reservation snapshot (RN-07).
 */
data class BookableUnit(
    val unitId: UUID,
    val publicationId: UUID,
    val status: String,
    val maxGuests: Int,
    val basePricePerNight: BigDecimal,
    val currency: String,
    val cancellationPolicy: String?,
)

interface UnitCatalogPort {

    fun findBookableUnit(unitId: UUID): BookableUnit?

    fun unitIdsOfHost(hostId: UUID): List<UUID>

    fun isHostOfUnit(unitId: UUID, hostId: UUID): Boolean
}

/**
 * Guest gate (RF-040: "la solicitud se crea solo cuando ... el perfil cumple
 * los requisitos mínimos"). Implemented by an adapter to identity's
 * UserVerificationQuery — a guest may request a stay only with COMPLETO
 * verification.
 */
interface GuestVerificationPort {

    fun hasMinimumVerification(guestUserId: UUID): Boolean
}
