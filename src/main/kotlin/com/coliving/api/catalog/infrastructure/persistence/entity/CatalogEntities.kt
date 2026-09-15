package com.coliving.api.catalog.infrastructure.persistence.entity

import com.coliving.api.catalog.domain.enums.CatalogCategory
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "catalog_entry",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_catalog_category_code", columnNames = ["category", "code"]),
    ],
)
class CatalogEntryEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    var category: CatalogCategory,

    @Column(name = "code", nullable = false, length = 60)
    var code: String,

    @Column(name = "name", nullable = false, length = 120)
    var name: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String?,

    // Self reference for the hierarchical categories (FACULTAD -> INSTITUCION,
    // CARRERA -> FACULTAD).
    @Column(name = "parent_id")
    var parentId: UUID?,

    @Column(name = "active", nullable = false)
    var active: Boolean,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)