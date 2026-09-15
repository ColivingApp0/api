package com.coliving.api.catalog.infrastructure.persistence

import com.coliving.api.catalog.domain.model.CatalogEntry
import com.coliving.api.catalog.infrastructure.persistence.entity.CatalogEntryEntity
import java.time.Instant

/** Pure domain <-> entity mapping for the catalog context. */
object CatalogMappers {

    fun toEntity(domain: CatalogEntry): CatalogEntryEntity =
        CatalogEntryEntity(
            id = domain.id,
            category = domain.category,
            code = domain.code,
            name = domain.name,
            description = domain.description,
            parentId = domain.parentId,
            active = domain.active,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    fun copyInto(entity: CatalogEntryEntity, domain: CatalogEntry): CatalogEntryEntity {
        entity.code = domain.code
        entity.name = domain.name
        entity.description = domain.description
        entity.parentId = domain.parentId
        entity.active = domain.active
        entity.updatedAt = Instant.now()
        return entity
    }

    fun toDomain(entity: CatalogEntryEntity): CatalogEntry =
        CatalogEntry(
            id = entity.id,
            category = entity.category,
            code = entity.code,
            name = entity.name,
            description = entity.description,
            parentId = entity.parentId,
            active = entity.active,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )

    fun toDomainList(entities: List<CatalogEntryEntity>): List<CatalogEntry> =
        entities.map { toDomain(it) }
}