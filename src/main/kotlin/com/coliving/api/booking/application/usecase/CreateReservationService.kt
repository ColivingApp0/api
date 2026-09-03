package com.coliving.api.booking.application.usecase

import com.coliving.api.booking.application.dto.CreateReservationCommand
import com.coliving.api.booking.application.dto.ReservationView
import com.coliving.api.booking.application.port.out.GuestVerificationPort
import com.coliving.api.booking.application.port.out.UnitAvailabilityPort
import com.coliving.api.booking.application.port.out.UnitCatalogPort
import com.coliving.api.booking.domain.enums.CancellationPolicyType
import com.coliving.api.booking.domain.enums.ReservationEventType
import com.coliving.api.booking.domain.model.Reservation
import com.coliving.api.booking.domain.model.ReservationEvent
import com.coliving.api.booking.domain.repository.ReservationEventRepository
import com.coliving.api.booking.domain.repository.ReservationRepository
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates a reservation request (RF-040). Guards, in order:
 *
 * 1. Unit exists and its publication is PUBLICADA (RN-03).
 * 2. Dates in the future and guests within the unit capacity.
 * 3. Guest profile meets the minimum verification level (RF-040).
 * 4. The unit's range is free (read check; the *enforcing* lock happens on
 *    host acceptance, per RF-043's provisional-reserve flow).
 *
 * Economic conditions are snapshotted (RN-07): price per night, currency and
 * cancellation policy are copied from the publication at request time.
 */
@Service
class CreateReservationService(
    private val reservationRepository: ReservationRepository,
    private val reservationEventRepository: ReservationEventRepository,
    internal val unitCatalogPort: UnitCatalogPort,
    private val unitAvailabilityPort: UnitAvailabilityPort,
    private val guestVerificationPort: GuestVerificationPort,
) {

    @Transactional
    fun create(command: CreateReservationCommand): ReservationView {
        val now = Instant.now()
        val bookable = unitCatalogPort.findBookableUnit(command.unitId)
            ?: throw NotFoundException("Unit not found or without publication")
        if (bookable.status != "PUBLICADA") {
            throw ConflictException("The publication does not admit requests (status=${bookable.status})")
        }
        if (bookable.maxGuests < command.occupants) {
            throw ConflictException(
                "Unit capacity is ${bookable.maxGuests}, requested $command.occupants occupants",
            )
        }
        if (!guestVerificationPort.hasMinimumVerification(command.guestId)) {
            throw ForbiddenException("Guest verification level does not meet the minimum requirement")
        }
        if (!unitAvailabilityPort.isRangeAvailable(command.unitId, command.fromDate, command.toDate)) {
            throw ConflictException("The requested dates are not available")
        }

        val reservation = Reservation.request(
            id = UUID.randomUUID(),
            unitId = command.unitId,
            publicationId = bookable.publicationId,
            guestUserId = command.guestId,
            fromDate = command.fromDate,
            toDate = command.toDate,
            occupants = command.occupants,
            message = command.message,
            pricePerNight = bookable.basePricePerNight,
            currency = bookable.currency,
            cancellationPolicy = bookable.cancellationPolicy?.let { policyName ->
                runCatching { CancellationPolicyType.valueOf(policyName) }.getOrNull()
            },
            now = now,
        )
        reservationRepository.save(reservation)
        recordEvent(reservation.id, ReservationEventType.CREADA, command.guestId, null, now)
        return toView(reservation)
    }

    internal fun recordEvent(
        reservationId: UUID,
        type: ReservationEventType,
        actorId: UUID,
        reason: String?,
        now: Instant,
    ) {
        reservationEventRepository.save(
            ReservationEvent(
                id = UUID.randomUUID(),
                reservationId = reservationId,
                type = type,
                actorUserId = actorId,
                reason = reason,
                occurredAt = now,
            ),
        )
    }

    internal fun loadReservation(reservationId: UUID): Reservation =
        reservationRepository.findById(reservationId)
            ?: throw NotFoundException("Reservation not found")

    internal fun requireGuest(reservation: Reservation, guestId: UUID) {
        if (reservation.guestUserId != guestId) {
            throw ForbiddenException("Not the guest of this reservation")
        }
    }

    internal fun requireHostOfUnit(unitId: UUID, hostId: UUID) {
        if (!unitCatalogPort.isHostOfUnit(unitId, hostId)) {
            throw ForbiddenException("Not the host of this unit")
        }
    }

    internal fun toView(reservation: Reservation): ReservationView =
        ReservationView(
            id = reservation.id,
            unitId = reservation.unitId,
            publicationId = reservation.publicationId,
            guestUserId = reservation.guestUserId,
            fromDate = reservation.fromDate,
            toDate = reservation.toDate,
            occupants = reservation.occupants,
            message = reservation.message,
            status = reservation.status,
            pricePerNight = reservation.pricePerNight,
            currency = reservation.currency,
            totalPrice = reservation.totalPrice,
            cancellationPolicy = reservation.cancellationPolicy,
            holdExpiresAt = reservation.holdExpiresAt,
            createdAt = reservation.createdAt,
        )
}
