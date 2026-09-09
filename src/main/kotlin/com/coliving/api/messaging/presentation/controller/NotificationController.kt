package com.coliving.api.messaging.presentation.controller

import com.coliving.api.messaging.application.dto.NotificationInboxView
import com.coliving.api.messaging.application.dto.NotificationView
import com.coliving.api.messaging.application.usecase.NotificationService
import com.coliving.api.shared.security.CurrentUser
import java.util.UUID
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * In-app notification center of the authenticated user (RF-051): the inbox with
 * its unread counter and the acknowledgement of a single notification. Only the
 * owner can read or mark one, enforced by the domain.
 */
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService,
) {

    @GetMapping
    fun inbox(@AuthenticationPrincipal current: CurrentUser): NotificationInboxView =
        notificationService.inbox(current.userId)

    @PostMapping("/{id}/read")
    fun markRead(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ): NotificationView = notificationService.markRead(id, current.userId)
}