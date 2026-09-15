package com.coliving.api.catalog.infrastructure.persistence.repository

import com.coliving.api.catalog.domain.enums.CatalogCategory
import com.coliving.api.catalog.infrastructure.persistence.entity.CatalogEntryEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface CatalogEntryJpaRepository : JpaRepository<CatalogEntryEntity, UUID> {

    fun findByCategoryOrderByNameAsc(category: CatalogCategory): List<CatalogEntryEntity>

    fun findByCategoryAndActiveOrderByNameAsc(
        category: CatalogCategory,
        active: Boolean,
    ): List<CatalogEntryEntity>

    fun findByCategoryAndCode(category: CatalogCategory, code: String): CatalogEntryEntity?

    fun findByIdIn(ids: List<UUID>): List<CatalogEntryEntity>
}