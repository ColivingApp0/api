package com.coliving.api.catalog.domain.repository

import com.coliving.api.catalog.domain.enums.CatalogCategory
import com.coliving.api.catalog.domain.model.CatalogEntry
import java.util.UUID

interface CatalogRepository {

    fun findById(id: UUID): CatalogEntry?

    /** Entries of a category, ordered by name; only active when requested. */
    fun findByCategory(category: CatalogCategory, activeOnly: Boolean): List<CatalogEntry>

    fun findByCategoryAndCode(category: CatalogCategory, code: String): CatalogEntry?

    fun findByIds(ids: List<UUID>): List<CatalogEntry>

    fun save(entry: CatalogEntry)
}