package com.coliving.api.search.application.usecase

import com.coliving.api.search.application.port.out.PublicationCatalogPort
import com.coliving.api.search.domain.model.Favorite
import com.coliving.api.search.domain.repository.FavoriteRepository
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Favorites of a user over published listings (RF-034). Only PUBLICADA
 * publications can be bookmarked; duplicates are rejected by the domain.
 */
@Service
class FavoriteService(
    private val favoriteRepository: FavoriteRepository,
    private val publicationCatalogPort: PublicationCatalogPort,
) {

    @Transactional
    fun add(userId: UUID, publicationId: UUID): Favorite {
        if (!publicationCatalogPort.isPublished(publicationId)) {
            throw NotFoundException("Published publication not found")
        }
        val favorite = Favorite.create(
            id = UUID.randomUUID(),
            userId = userId,
            publicationId = publicationId,
            existingForUser = favoriteRepository.findIdsByUser(userId),
            now = Instant.now(),
        )
        favoriteRepository.save(favorite)
        return favorite
    }

    @Transactional
    fun remove(userId: UUID, publicationId: UUID) {
        if (!favoriteRepository.existsByUserAndPublication(userId, publicationId)) {
            throw NotFoundException("Favorite not found for this user")
        }
        favoriteRepository.delete(userId, publicationId)
    }

    @Transactional(readOnly = true)
    fun listMine(userId: UUID): List<Favorite> = favoriteRepository.findByUser(userId)
}
