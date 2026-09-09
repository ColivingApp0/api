package com.coliving.api.messaging.presentation.controller

import com.coliving.api.messaging.application.dto.ConversationDetailView
import com.coliving.api.messaging.application.dto.ConversationView
import com.coliving.api.messaging.application.dto.MessageView
import com.coliving.api.messaging.application.dto.OpenConversationCommand
import com.coliving.api.messaging.application.dto.SendMessageCommand
import com.coliving.api.messaging.application.dto.SendMessageToReservationCommand
import com.coliving.api.messaging.application.usecase.ConversationService
import com.coliving.api.messaging.application.usecase.MessageService
import com.coliving.api.messaging.presentation.dto.OpenConversationRequest
import com.coliving.api.messaging.presentation.dto.SendMessageRequest
import com.coliving.api.shared.security.CurrentUser
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
 * Conversation and messaging endpoints (RF-050). Any authenticated user may
 * call them: who actually is a party of a reservation is decided by the
 * application service, not by a role, since both the guest and the host use the
 * same thread.
 */
@RestController
@RequestMapping("/api/v1/conversations")
class ConversationController(
    private val conversationService: ConversationService,
    private val messageService: MessageService,
) {

    /** Threads of the authenticated user (guest or host side). */
    @GetMapping
    fun listMine(@AuthenticationPrincipal current: CurrentUser): List<ConversationView> =
        conversationService.listMine(current.userId)

    /** Opens — or reopens — the conversation of a reservation (idempotent). */
    @PostMapping
    fun open(
        @AuthenticationPrincipal current: CurrentUser,
        @Valid @RequestBody request: OpenConversationRequest,
    ): ConversationDetailView = conversationService.open(
        OpenConversationCommand(reservationId = request.reservationId, actorId = current.userId),
    )

    /** Thread with its messages in chronological order. */
    @GetMapping("/{id}")
    fun get(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): ConversationDetailView = conversationService.get(id, current.userId)

    /** Writes a message inside an existing thread. */
    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    fun send(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: SendMessageRequest,
    ): MessageView = messageService.send(
        SendMessageCommand(conversationId = id, senderUserId = current.userId, body = request.body),
    )
}

/**
 * Convenience entry point for clients that only know the reservation: the
 * thread is opened on demand and the message lands in it.
 */
@RestController
@RequestMapping("/api/v1/reservations")
class ReservationMessageController(
    private val messageService: MessageService,
) {

    @PostMapping("/{reservationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    fun send(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable reservationId: UUID,
        @Valid @RequestBody request: SendMessageRequest,
    ): MessageView = messageService.sendToReservation(
        SendMessageToReservationCommand(
            reservationId = reservationId,
            senderUserId = current.userId,
            body = request.body,
        ),
    )
}