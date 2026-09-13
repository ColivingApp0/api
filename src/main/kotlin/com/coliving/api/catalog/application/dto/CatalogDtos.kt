package com.coliving.api.catalog.application.dto

import com.coliving.api.catalog.domain.enums.CatalogCategory
import java.time.Instant
import java.util.UUID

/** A managed catalog entry as seen by the administrator (RF-083). */
data class CatalogEntryView(
    val id: UUID,
    val category: CatalogCategory,
    val code: String,
    val name: String,
    val description: String?,
    val parentId: UUID?,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Creates a catalog entry of the given category (RF-083). */
data class CreateCatalogEntryCommand(
    val category: CatalogCategory,
    val code: String,
    val name: String,
    val description: String? = null,
    val parentId: UUID? = null,
)

/** Updates the editable fields of a catalog entry. */
data class UpdateCatalogEntryCommand(
    val entryId: UUID,
    val name: String,
    val description: String?,
)