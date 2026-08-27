package com.coliving.api.accommodation.application.usecase

import com.coliving.api.accommodation.application.dto.BlockDatesCommand
import com.coliving.api.accommodation.application.dto.DayAvailabilityView
import com.coliving.api.accommodation.application.dto.UnblockDatesCommand
import com.coliving.api.accommodation.application.port.out.ReservationAvailabilityPort
import com.coliving.api.accommodation.domain.enums.AvailabilityState
import com.coliving.api.accommodation.domain.model.Unit
import com.coliving.api.accommodation.domain.repository.AvailabilitySlotRepository
import com.coliving.api.accommodation.domain.repository.PropertyRepository
import com.coliving.api.accommodation.domain.repository.UnitRepository
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.InvalidArgumentException
import com.coliving.api.shared.error.NotFoundException
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Availability operations. The locking discipline (`findOrCreateRange` +
 * `SELECT ... FOR UPDATE` in ascending date order inside one transaction) is
 * what makes double booking impossible. Host blocks use reservationId = null;
 * future bookings use [lockRange]/[unlockRange] and arrive through an adapter
 * implementing [ReservationAvailabilityPort].
 */
@Service
class AvailabilityLockService(
    private val slotRepository: AvailabilitySlotRepository,
    private val unitRepository: UnitRepository,
    private val propertyRepository: PropertyRepository,
) : ReservationAvailabilityPort {

    @Transactional
    override fun lockRange(unitId: UUID, from: LocalDate, to: LocalDate, reservationId: UUID) {
        requireUnitExists(unitId)
        val range = validateRange(from, to)
        val slots = slotRepository.findOrCreateRange(unitId, range.first, range.second)
        slots.forEach { it.lock(reservationId) }
        slotRepository.saveAll(slots)
    }

    @Transactional
    override fun unlockRange(unitId: UUID, from: LocalDate, to: LocalDate, reservationId: UUID) {
        requireUnitExists(unitId)
        val range = validateRange(from, to)
        val slots = slotRepository.findRange(unitId, range.first, range.second)
        // Only release rows held by this reservation (never another party's).
        slots.filter {
            it.reservationId == reservationId && it.state != AvailabilityState.DISPONIBLE
        }.forEach { it.release() }
        slotRepository.saveAll(slots)
    }

    override fun isRangeAvailable(unitId: UUID, from: LocalDate, to: LocalDate): Boolean {
        val range = validateRange(from, to)
        return slotRepository.isRangeAvailable(unitId, range.first, range.second)
    }

    @Transactional
    fun blockDates(command: BlockDatesCommand) {
        val unit = requireOwnedUnit(command.unitId, command.hostId)
        val range = validateRange(command.from, command.to)
        val slots = slotRepository.findOrCreateRange(unit.id, range.first, range.second)
        slots.forEach { it.blockByHost() }
        slotRepository.saveAll(slots)
    }

    @Transactional
    fun unblockDates(command: UnblockDatesCommand) {
        val unit = requireOwnedUnit(command.unitId, command.hostId)
        val range = validateRange(command.from, command.to)
        val slots = slotRepository.findRange(unit.id, range.first, range.second)
        // Only release host blocks (reservation_id = null), never reservation holds.
        slots.filter {
            it.reservationId == null && it.state != AvailabilityState.DISPONIBLE
        }.forEach { it.release() }
        slotRepository.saveAll(slots)
    }

    fun getAvailability(unitId: UUID, from: LocalDate, to: LocalDate): List<DayAvailabilityView> {
        val range = validateRange(from, to)
        val byDate = slotRepository.findRangeReadOnly(unitId, range.first, range.second)
        return (0 until ChronoUnit.DAYS.between(range.first, range.second))
            .map { offset -> range.first.plusDays(offset) }
            .map { date -> byDate.getValue(date) }
            .map { slot ->
                DayAvailabilityView(
                    date = slot.date,
                    state = slot.state,
                    reserved = slot.reservationId != null,
                )
            }
    }

    private fun requireOwnedUnit(unitId: UUID, hostId: UUID): Unit {
        val unit = unitRepository.findById(unitId)
            ?: throw NotFoundException("Unit not found")
        val property = propertyRepository.findById(unit.propertyId)
            ?: throw NotFoundException("Property not found")
        if (property.hostId != hostId) throw ForbiddenException("Not the owner of this unit")
        return unit
    }

    private fun requireUnitExists(unitId: UUID) {
        if (unitRepository.findById(unitId) == null) throw NotFoundException("Unit not found")
    }

    private fun validateRange(from: LocalDate, to: LocalDate): Pair<LocalDate, LocalDate> {
        if (to <= from) throw InvalidArgumentException("to must be after from")
        if (ChronoUnit.DAYS.between(from, to) > 366) {
            throw InvalidArgumentException("Range too large (max 366 nights)")
        }
        return from to to
    }
}