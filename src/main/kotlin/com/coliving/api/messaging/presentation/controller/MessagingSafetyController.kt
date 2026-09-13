package com.coliving.api.messaging.presentation.controller

import com.coliving.api.messaging.application.dto.BlockView
import com.coliving.api.messaging.application.dto.ConversationReportView
import com.coliving.api.messaging.application.dto.ReportConversationCommand
import com.coliving.api.messaging.application.usecase.BlockService
import com.coliving.api.messaging.application.usecase.ReportService
import com.coliving.api.messaging.presentation.dto.BlockUserRequest
import com.coliving.api.messaging.presentation.dto.ReportConversationRequest
import com.coliving.api.shared.security.CurrentUser
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Community safety endpoints of messaging (RF-052, RF-053): reporting a
 * conversation opens a support case in moderation, and blocking a user stops
 * the exchange in both directions.
 */
@RestController
class MessagingSafetyController(
    private val blockService: BlockService,
    private val reportService: ReportService,
) {

    @PostMapping("/api/v1/conversations/{id}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    fun report(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ReportConversationRequest,
    ): ConversationReportView = reportService.report(
        ReportConversationCommand(
            conversationId = id,
            reporterUserId = current.userId,
            reason = request.reason,
        ),
    )

    /** Reports raised by the authenticated user, for follow-up. */
    @GetMapping("/api/v1/conversations/reports")
    fun listMyReports(@AuthenticationPrincipal current: CurrentUser): List<ConversationReportView> =
        reportService.listMine(current.userId)

    @PostMapping("/api/v1/users/me/blocks")
    @ResponseStatus(HttpStatus.CREATED)
    fun block(
        @AuthenticationPrincipal current: CurrentUser,
        @Valid @RequestBody request: BlockUserRequest,
    ): BlockView = blockService.block(current.userId, request.blockedUserId)

    @DeleteMapping("/api/v1/users/me/blocks/{blockedUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unblock(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable blockedUserId: UUID,
    ) = blockService.unblock(current.userId, blockedUserId)

    @GetMapping("/api/v1/users/me/blocks")
    fun listMyBlocks(@AuthenticationPrincipal current: CurrentUser): List<BlockView> =
        blockService.listMine(current.userId)
}