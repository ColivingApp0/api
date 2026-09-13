package com.coliving.api.catalog.application.usecase

import com.coliving.api.catalog.application.dto.CatalogEntryView
import com.coliving.api.catalog.application.dto.CreateCatalogEntryCommand
import com.coliving.api.catalog.application.dto.UpdateCatalogEntryCommand
import com.coliving.api.catalog.domain.enums.CatalogCategory
import com.coliving.api.catalog.domain.model.CatalogEntry
import com.coliving.api.catalog.domain.repository.CatalogRepository
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Administration of the managed catalogs (RF-083): institutions, faculties,
 * careers, cities, services, rules, room types and accessibility features.
 * Codes are unique per category, hierarchical categories validate their
 * parent, and entries are deactivated — never deleted — so the plain-UUID
 * references held by the other contexts always resolve.
 */
@Service
class CatalogService(
    private val catalogRepository: CatalogRepository,
) {

    @Transactional
    fun create(command: CreateCatalogEntryCommand): CatalogEntryView {
        val normalizedCode = command.code.trim().uppercase()
        if (catalogRepository.findByCategoryAndCode(command.category, normalizedCode) != null) {
            throw ConflictException("A ${command.category} entry with that code already exists")
        }
        val parent = command.parentId?.let { parentId ->
            catalogRepository.findById(parentId) ?: throw NotFoundException("Parent catalog entry not found")
        }
        val entry = CatalogEntry.create(
            id = UUID.randomUUID(),
            category = command.category,
            code = normalizedCode,
            name = command.name,
            description = command.description,
            parentId = command.parentId,
            parentCategory = parent?.category,
            now = Instant.now(),
        )
        catalogRepository.save(entry)
        return entry.toView()
    }

    @Transactional
    fun update(command: UpdateCatalogEntryCommand): CatalogEntryView {
        val entry = catalogRepository.findById(command.entryId)
            ?: throw NotFoundException("Catalog entry not found")
        entry.update(command.name, command.description, Instant.now())
        catalogRepository.save(entry)
        return entry.toView()
    }

    @Transactional
    fun deactivate(entryId: UUID): CatalogEntryView {
        val entry = catalogRepository.findById(entryId)
            ?: throw NotFoundException("Catalog entry not found")
        entry.deactivate(Instant.now())
        catalogRepository.save(entry)
        return entry.toView()
    }

    @Transactional
    fun activate(entryId: UUID): CatalogEntryView {
        val entry = catalogRepository.findById(entryId)
            ?: throw NotFoundException("Catalog entry not found")
        entry.activate(Instant.now())
        catalogRepository.save(entry)
        return entry.toView()
    }

    @Transactional(readOnly = true)
    fun list(category: CatalogCategory, activeOnly: Boolean): List<CatalogEntryView> =
        catalogRepository.findByCategory(category, activeOnly).map { it.toView() }

    /** Whether every id belongs to an active entry of the category (RF-031 guard). */
    @Transactional(readOnly = true)
    fun allActive(category: CatalogCategory, ids: Collection<UUID>): Boolean {
        if (ids.isEmpty()) return true
        val found = catalogRepository.findByIds(ids.toList())
            .filter { it.category == category && it.active }
            .map { it.id }
            .toSet()
        return found.containsAll(ids)
    }
}

/** Shared catalog projection. */
internal fun CatalogEntry.toView(): CatalogEntryView =
    CatalogEntryView(
        id = id,
        category = category,
        code = code,
        name = name,
        description = description,
        parentId = parentId,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )