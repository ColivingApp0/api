package com.coliving.api.booking.presentation.controller

import com.coliving.api.booking.application.dto.CancelReservationCommand
import com.coliving.api.booking.application.dto.HostDecisionCommand
import com.coliving.api.booking.application.dto.ReservationEventView
import com.coliving.api.booking.application.dto.ReservationView
import com.coliving.api.booking.application.usecase.AcceptReservationService
import com.coliving.api.booking.application.usecase.CancelReservationService
import com.coliving.api.booking.application.usecase.ListReservationsService
import com.coliving.api.booking.application.usecase.RejectReservationService
import com.coliving.api.booking.application.usecase.RequestInfoReservationService
import com.coliving.api.booking.presentation.dto.CancelRequest
import com.coliving.api.booking.presentation.dto.DecisionRequest
import com.coliving.api.shared.security.CurrentUser
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Host endpoints for incoming reservations (ANFITRION only). Decisions map to
 * the RF-041 contract: accept, reject or request information; cancellation is
 * shared with the guest (RF-044).
 */
@RestController
@RequestMapping("/api/v1/host/reservations")
class HostBookingController(
    private val acceptReservationService: AcceptReservationService,
    private val rejectReservationService: RejectReservationService,
    private val requestInfoReservationService: RequestInfoReservationService,
    private val cancelReservationService: CancelReservationService,
    private val listReservationsService: ListReservationsService,
) {

    @GetMapping
    fun incoming(@AuthenticationPrincipal current: CurrentUser): List<ReservationView> =
        requireHost(current) { listReservationsService.listForHost(current.userId) }

    @GetMapping("/{id}/history")
    fun history(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): List<ReservationEventView> = requireHost(current) {
        listReservationsService.history(id, current.userId)
    }

    @PostMapping("/{id}/accept")
    fun accept(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: DecisionRequest,
    ): ReservationView = requireHost(current) {
        acceptReservationService.accept(
            HostDecisionCommand(reservationId = id, hostId = current.userId, reason = request.reason),
        )
    }

    @PostMapping("/{id}/reject")
    fun reject(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: DecisionRequest,
    ): ReservationView = requireHost(current) {
        rejectReservationService.reject(
            HostDecisionCommand(reservationId = id, hostId = current.userId, reason = request.reason),
        )
    }

    @PostMapping("/{id}/request-info")
    fun requestInfo(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: DecisionRequest,
    ): ReservationView = requireHost(current) {
        requestInfoReservationService.requestInfo(
            HostDecisionCommand(reservationId = id, hostId = current.userId, reason = request.reason),
        )
    }

    @PostMapping("/{id}/cancel")
    fun cancel(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: CancelRequest,
    ): ReservationView = requireHost(current) {
        cancelReservationService.cancel(
            CancelReservationCommand(reservationId = id, actorId = current.userId, reason = request.reason),
        )
    }

    private fun <T> requireHost(current: CurrentUser, block: () -> T): T {
        if (!current.roleCodes.contains("ANFITRION")) {
            throw AccessDeniedException("ANFITRION role required")
        }
        return block()
    }
}
