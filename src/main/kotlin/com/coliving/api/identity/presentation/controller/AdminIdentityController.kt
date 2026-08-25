package com.coliving.api.identity.presentation.controller

import com.coliving.api.identity.application.dto.AssignRoleCommand
import com.coliving.api.identity.application.dto.DocumentSummary
import com.coliving.api.identity.application.dto.ReviewAction
import com.coliving.api.identity.application.dto.ReviewDocumentCommand
import com.coliving.api.identity.application.dto.RoleSummary
import com.coliving.api.identity.application.dto.SelectRoleResult
import com.coliving.api.identity.application.usecase.AssignRoleService
import com.coliving.api.identity.application.usecase.ReviewVerificationDocumentService
import com.coliving.api.identity.domain.enums.DocumentStatus
import com.coliving.api.shared.security.CurrentUser
import com.coliving.api.identity.presentation.dto.AssignRoleRequest
import com.coliving.api.identity.presentation.dto.ReviewDocumentRequest
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Internal moderation/admin endpoints (RF-012, RF-014). Access is restricted to
 * MODERADOR / ADMINISTRADOR at the security layer.
 */
@RestController
@RequestMapping("/api/v1/admin")
class AdminIdentityController(
    private val reviewVerificationDocumentService: ReviewVerificationDocumentService,
    private val assignRoleService: AssignRoleService,
) {

    @GetMapping("/verification-documents")
    fun listDocuments(
        @RequestParam(required = false) status: DocumentStatus?,
    ): List<DocumentSummary> = reviewVerificationDocumentService.listByStatus(status)

    @PostMapping("/verification-documents/{id}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun approveDocument(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
    ) {
        reviewVerificationDocumentService.review(
            ReviewDocumentCommand(
                documentId = id,
                reviewerUserId = current.userId,
                action = ReviewAction.APPROVE,
            ),
        )
    }

    @PostMapping("/verification-documents/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun rejectDocument(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ReviewDocumentRequest,
    ) {
        reviewVerificationDocumentService.review(
            ReviewDocumentCommand(
                documentId = id,
                reviewerUserId = current.userId,
                action = ReviewAction.REJECT,
                reason = request.reason,
            ),
        )
    }

    @PostMapping("/verification-documents/{id}/request-correction")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun requestCorrection(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ReviewDocumentRequest,
    ) {
        reviewVerificationDocumentService.review(
            ReviewDocumentCommand(
                documentId = id,
                reviewerUserId = current.userId,
                action = ReviewAction.REQUEST_CORRECTION,
                reason = request.reason,
            ),
        )
    }

    @PostMapping("/users/{userId}/roles")
    fun assignRole(
        @PathVariable userId: UUID,
        @Valid @RequestBody request: AssignRoleRequest,
    ): SelectRoleResult = assignRoleService.assign(
        AssignRoleCommand(
            userId = userId,
            roleCode = request.roleCode ?: error("roleCode is required"),
        ),
    )

    @GetMapping("/roles")
    fun listRoles(): List<RoleSummary> = assignRoleService.listRoles()
}