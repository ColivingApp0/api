package com.coliving.api.search.domain.repository

import com.coliving.api.search.domain.model.Favorite
import java.util.UUID

interface FavoriteRepository {
    fun findByUser(userId: UUID): List<Favorite>
    fun findIdsByUser(userId: UUID): Set<UUID>
    fun existsByUserAndPublication(userId: UUID, publicationId: UUID): Boolean
    fun save(favorite: Favorite)
    fun delete(userId: UUID, publicationId: UUID)
}
