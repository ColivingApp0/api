package com.coliving.api.booking.presentation.controller

import com.coliving.api.booking.application.dto.CancelReservationCommand
import com.coliving.api.booking.application.dto.ConfirmReservationCommand
import com.coliving.api.booking.application.dto.CreateReservationCommand
import com.coliving.api.booking.application.dto.ReservationView
import com.coliving.api.booking.application.usecase.CancelReservationService
import com.coliving.api.booking.application.usecase.ConfirmReservationService
import com.coliving.api.booking.application.usecase.CreateReservationService
import com.coliving.api.booking.application.usecase.ListReservationsService
import com.coliving.api.booking.presentation.dto.CancelRequest
import com.coliving.api.booking.presentation.dto.CreateReservationRequest
import com.coliving.api.shared.security.CurrentUser
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Guest endpoints for reservations (HUESPED_* roles). Ownership is enforced
 * inside the application services; role is checked here, mirroring the host
 * controller.
 */
@Tag(
    name = "Reservations",
    description = "Guest endpoints for reservations (HUESPED_* roles): create, confirm, cancel and list (RF-040, RF-043, RF-044).",
)
@RestController
@RequestMapping("/api/v1/reservations")
class GuestBookingController(
    private val createReservationService: CreateReservationService,
    private val confirmReservationService: ConfirmReservationService,
    private val cancelReservationService: CancelReservationService,
    private val listReservationsService: ListReservationsService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal current: CurrentUser,
        @Valid @RequestBody request: CreateReservationRequest,
    ): ReservationView = requireGuest(current) {
        createReservationService.create(
            CreateReservationCommand(
                guestId = current.userId,
                unitId = request.unitId,
                fromDate = request.fromDate,
                toDate = request.toDate,
                occupants = request.occupants,
                message = request.message,
            ),
        )
    }

    @GetMapping("/mine")
    fun listMine(@AuthenticationPrincipal current: CurrentUser): List<ReservationView> =
        requireGuest(current) { listReservationsService.listMine(current.userId) }

    @PostMapping("/{id}/confirm")
    fun confirm(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): ReservationView = requireGuest(current) {
        confirmReservationService.confirm(
            ConfirmReservationCommand(reservationId = id, guestId = current.userId),
        )
    }

    @PostMapping("/{id}/cancel")
    fun cancel(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: CancelRequest,
    ): ReservationView = requireGuest(current) {
        cancelReservationService.cancel(
            CancelReservationCommand(reservationId = id, actorId = current.userId, reason = request.reason),
        )
    }

    private fun <T> requireGuest(current: CurrentUser, block: () -> T): T {
        if (current.roleCodes.none { it.startsWith("HUESPED") }) {
            throw org.springframework.security.access.AccessDeniedException("HUESPED role required")
        }
        return block()
    }
}
