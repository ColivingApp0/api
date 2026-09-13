package com.coliving.api.catalog.domain.model

import com.coliving.api.catalog.domain.enums.CatalogCategory
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.InvalidArgumentException
import java.time.Instant
import java.util.UUID

/**
 * One catalog entry (RF-083): a managed reference value — an institution, a
 * faculty, a career, a city, a service, a rule, a room type or an
 * accessibility feature. Entries are deactivated, never deleted, so the
 * cross-context references (plain UUIDs, no FK) always resolve.
 */
class CatalogEntry(
    val id: UUID,
    val category: CatalogCategory,
    var code: String,
    var name: String,
    var description: String?,
    var parentId: UUID?,
    var active: Boolean,
    val createdAt: Instant,
    var updatedAt: Instant,
) {

    fun update(name: String, description: String?, now: Instant) {
        this.name = requireName(name)
        this.description = description?.trim()?.takeIf { it.isNotEmpty() }
        updatedAt = now
    }

    /** Deactivates the entry without deleting it (references survive). */
    fun deactivate(now: Instant) {
        if (!active) {
            throw ConflictException("The catalog entry is already inactive")
        }
        active = false
        updatedAt = now
    }

    fun activate(now: Instant) {
        if (active) {
            throw ConflictException("The catalog entry is already active")
        }
        active = true
        updatedAt = now
    }

    companion object {
        const val MAX_CODE_LENGTH = 60
        const val MAX_NAME_LENGTH = 120

        /**
         * Creates an entry. `parentId` is only admitted for hierarchical
         * categories and must point to an active entry of the parent category.
         */
        fun create(
            id: UUID,
            category: CatalogCategory,
            code: String,
            name: String,
            description: String?,
            parentId: UUID?,
            parentCategory: CatalogCategory?,
            now: Instant,
        ): CatalogEntry {
            val normalizedCode = code.trim().uppercase()
            if (normalizedCode.isEmpty()) {
                throw InvalidArgumentException("The catalog code is required")
            }
            if (normalizedCode.length > MAX_CODE_LENGTH) {
                throw InvalidArgumentException("The catalog code must not exceed $MAX_CODE_LENGTH characters")
            }
            if (normalizedCode.any { it == ' ' }) {
                throw InvalidArgumentException("The catalog code must not contain spaces")
            }

            val expectedParent = category.parentCategory()
            if (expectedParent == null) {
                if (parentId != null) {
                    throw InvalidArgumentException("$category entries must not have a parent")
                }
            } else {
                if (parentId == null) {
                    throw InvalidArgumentException("A $category entry requires a $expectedParent parent")
                }
                if (parentCategory != expectedParent) {
                    throw InvalidArgumentException("The parent must be a $expectedParent entry")
                }
            }

            return CatalogEntry(
                id = id,
                category = category,
                code = normalizedCode,
                name = requireName(name),
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                parentId = parentId,
                active = true,
                createdAt = now,
                updatedAt = now,
            )
        }

        internal fun requireName(name: String): String {
            val normalized = name.trim()
            if (normalized.isEmpty()) {
                throw InvalidArgumentException("The catalog name is required")
            }
            if (normalized.length > MAX_NAME_LENGTH) {
                throw InvalidArgumentException("The catalog name must not exceed $MAX_NAME_LENGTH characters")
            }
            return normalized
        }
    }
}