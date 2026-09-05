package com.coliving.api.search.domain.model

import com.coliving.api.shared.error.ConflictException
import java.time.Instant
import java.util.UUID

/**
 * Bookmark of a published listing by a user (RF-034). References the
 * publication by plain UUID (no cross-context relations). A user cannot
 * bookmark the same publication twice.
 */
class Favorite(
    val id: UUID,
    val userId: UUID,
    val publicationId: UUID,
    val createdAt: Instant,
) {
    companion object {
        fun create(
            id: UUID,
            userId: UUID,
            publicationId: UUID,
            existingForUser: Set<UUID>,
            now: Instant,
        ): Favorite {
            if (publicationId in existingForUser) {
                throw ConflictException("Publication is already a favorite of this user")
            }
            return Favorite(id = id, userId = userId, publicationId = publicationId, createdAt = now)
        }
    }
}
