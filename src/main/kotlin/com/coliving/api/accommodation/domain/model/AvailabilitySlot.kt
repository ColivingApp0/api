package com.coliving.api.accommodation.domain.model

import com.coliving.api.accommodation.domain.enums.AvailabilityState
import com.coliving.api.shared.error.ConflictException
import java.time.LocalDate
import java.util.UUID

/**
 * Per-day availability of a unit. State transitions are the only way to mutate
 * the row (no setters): [lock]/[blockByHost] require [AvailabilityState.DISPONIBLE],
 * [confirm] requires [AvailabilityState.BLOQUEADO], and [release] returns to
 * [AvailabilityState.DISPONIBLE].
 *
 * [reservationId] is an **external reference** to the future booking context
 * (plain UUID, no relationship). Null means either a fresh/free slot or a
 * host/manual block.
 */
class AvailabilitySlot internal constructor(
    val unitId: UUID,
    val date: LocalDate,
    state: AvailabilityState,
    reservationId: UUID?,
) {

    /** Persistence hint for round-trips; never used by business rules. */
    internal var dbId: Long? = null

    var state: AvailabilityState = state
        private set

    var reservationId: UUID? = reservationId
        private set

    /** Locks free days on behalf of a (future) booking. */
    fun lock(reservationId: UUID) {
        requireAvailableForChange()
        this.state = AvailabilityState.BLOQUEADO
        this.reservationId = reservationId
    }

    /** Host/manual block: same state, no reservation reference. */
    fun blockByHost() {
        requireAvailableForChange()
        this.state = AvailabilityState.BLOQUEADO
        this.reservationId = null
    }

    /** Confirms a prior lock -> occupied by a confirmed reservation. */
    fun confirm() {
        check(state == AvailabilityState.BLOQUEADO) {
            "Cannot confirm a slot in state $state"
        }
        this.state = AvailabilityState.OCUPADO
    }

    /** Releases a lock or a confirmed day back to available. */
    fun release() {
        check(state == AvailabilityState.BLOQUEADO || state == AvailabilityState.OCUPADO) {
            "Cannot release a slot in state $state"
        }
        this.state = AvailabilityState.DISPONIBLE
        this.reservationId = null
    }

    private fun requireAvailableForChange() {
        if (state != AvailabilityState.DISPONIBLE) {
            throw ConflictException("Day $date is not available (state=$state)")
        }
    }

    companion object {
        fun of(
            unitId: UUID,
            date: LocalDate,
            state: AvailabilityState = AvailabilityState.DISPONIBLE,
            reservationId: UUID? = null,
            dbId: Long? = null,
        ): AvailabilitySlot = AvailabilitySlot(unitId, date, state, reservationId).also { it.dbId = dbId }
    }
}