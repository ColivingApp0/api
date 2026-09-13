package com.coliving.api.catalog.presentation.controller

import com.coliving.api.catalog.application.dto.CatalogEntryView
import com.coliving.api.catalog.application.dto.CreateCatalogEntryCommand
import com.coliving.api.catalog.application.dto.UpdateCatalogEntryCommand
import com.coliving.api.catalog.application.usecase.CatalogService
import com.coliving.api.catalog.domain.enums.CatalogCategory
import com.coliving.api.shared.security.CurrentUser
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Administration of the managed catalogs (RF-083). The admin namespace
 * requires MODERADOR or ADMINISTRADOR (SecurityConfig); the authenticated
 * internal user is recorded by the case/audit trail of each action.
 */
@Tag(
    name = "Admin Catalogs",
    description = "Managed catalogs (RF-083): institutions, faculties, careers, cities, services, rules, room types and accessibility features. Entries are deactivated, never deleted.",
)
@RestController
@RequestMapping("/api/v1/admin/catalogs")
class AdminCatalogController(
    private val catalogService: CatalogService,
) {

    data class CreateCatalogEntryRequest(
        @field:NotBlank(message = "code is required")
        @field:Size(max = 60, message = "code must not exceed 60 characters")
        val code: String,
        @field:NotBlank(message = "name is required")
        @field:Size(max = 120, message = "name must not exceed 120 characters")
        val name: String,
        @field:Size(max = 1000, message = "description must not exceed 1000 characters")
        val description: String? = null,
        @field:NotNull(message = "parentId is required for hierarchical categories")
        val parentId: UUID? = null,
    )

    data class UpdateCatalogEntryRequest(
        @field:NotBlank(message = "name is required")
        @field:Size(max = 120, message = "name must not exceed 120 characters")
        val name: String,
        @field:Size(max = 1000, message = "description must not exceed 1000 characters")
        val description: String? = null,
    )

    /** Entries of a category, active by default; the admin can list all. */
    @GetMapping("/{category}")
    fun list(
        @PathVariable category: CatalogCategory,
        @RequestParam(required = false, defaultValue = "true") activeOnly: Boolean,
    ): List<CatalogEntryView> = catalogService.list(category, activeOnly)

    @PostMapping("/{category}")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable category: CatalogCategory,
        @Valid @RequestBody request: CreateCatalogEntryRequest,
    ): CatalogEntryView = catalogService.create(
        CreateCatalogEntryCommand(
            category = category,
            code = request.code,
            name = request.name,
            description = request.description,
            parentId = request.parentId,
        ),
    )

    @PutMapping("/{category}/{id}")
    fun update(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable category: CatalogCategory,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateCatalogEntryRequest,
    ): CatalogEntryView = catalogService.update(
        UpdateCatalogEntryCommand(entryId = id, name = request.name, description = request.description),
    )

    /** Soft deactivation: references held by listings and profiles survive. */
    @DeleteMapping("/{category}/{id}")
    fun deactivate(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable category: CatalogCategory,
        @PathVariable id: UUID,
    ): CatalogEntryView = catalogService.deactivate(id)
}