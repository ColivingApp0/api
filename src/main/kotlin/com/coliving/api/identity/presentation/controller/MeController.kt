package com.coliving.api.identity.presentation.controller

import com.coliving.api.identity.application.dto.ConsentSummary
import com.coliving.api.identity.application.dto.CurrentUserResult
import com.coliving.api.identity.application.dto.DocumentSummary
import com.coliving.api.identity.application.dto.ManageConsentCommand
import com.coliving.api.identity.application.dto.SelectRoleCommand
import com.coliving.api.identity.application.dto.SelectRoleResult
import com.coliving.api.identity.application.dto.SubmitDocumentCommand
import com.coliving.api.identity.application.usecase.CurrentUserService
import com.coliving.api.identity.application.usecase.ManageConsentService
import com.coliving.api.identity.application.usecase.SelectRoleService
import com.coliving.api.identity.application.usecase.SubmitVerificationDocumentService
import com.coliving.api.identity.domain.enums.ConsentType
import com.coliving.api.identity.domain.enums.DocumentType
import com.coliving.api.identity.presentation.dto.ManageConsentRequest
import com.coliving.api.identity.presentation.dto.SelectRoleRequest
import com.coliving.api.shared.security.CurrentUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * Authenticated user endpoints (identity side): account, consents, role
 * selection and verification documents. Profile data now lives under
 * `/api/v1/profile` in the `profile` bounded context.
 */
@RestController
@RequestMapping("/api/v1/users/me")
class MeController(
    private val currentUserService: CurrentUserService,
    private val manageConsentService: ManageConsentService,
    private val selectRoleService: SelectRoleService,
    private val submitVerificationDocumentService: SubmitVerificationDocumentService,
) {

    @GetMapping
    fun me(@AuthenticationPrincipal current: CurrentUser): CurrentUserResult =
        currentUserService.current(current.userId)

    @PutMapping("/consents/{type}")
    fun manageConsent(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable type: ConsentType,
        @Valid @RequestBody request: ManageConsentRequest,
    ): ConsentSummary = manageConsentService.manage(
        ManageConsentCommand(
            userId = current.userId,
            type = type,
            version = request.version,
            purpose = request.purpose,
            accept = request.accept ?: false,
        ),
    )

    @PutMapping("/role")
    fun selectRole(
        @AuthenticationPrincipal current: CurrentUser,
        @Valid @RequestBody request: SelectRoleRequest,
    ): SelectRoleResult = selectRoleService.selectRole(
        SelectRoleCommand(
            userId = current.userId,
            roleCode = request.roleCode ?: error("roleCode is required"),
        ),
    )

    @PostMapping(
        value = ["/verification-documents"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    )
    @ResponseStatus(HttpStatus.CREATED)
    fun submitDocument(
        @AuthenticationPrincipal current: CurrentUser,
        @RequestParam("type") type: DocumentType,
        @RequestParam("file") file: MultipartFile,
    ): DocumentSummary = submitVerificationDocumentService.submit(
        SubmitDocumentCommand(
            userId = current.userId,
            type = type,
            fileName = file.originalFilename ?: "document",
            contentType = file.contentType ?: "application/octet-stream",
            bytes = file.bytes,
        ),
    )

    @GetMapping("/verification-documents")
    fun myDocuments(@AuthenticationPrincipal current: CurrentUser): List<DocumentSummary> =
        currentUserService.current(current.userId).documents
}