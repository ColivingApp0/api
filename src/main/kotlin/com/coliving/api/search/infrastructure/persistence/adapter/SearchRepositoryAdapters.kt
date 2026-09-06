package com.coliving.api.search.infrastructure.persistence.adapter

import com.coliving.api.search.domain.model.Favorite
import com.coliving.api.search.domain.repository.FavoriteRepository
import com.coliving.api.search.infrastructure.persistence.entity.FavoriteEntity
import com.coliving.api.search.infrastructure.persistence.repository.FavoriteJpaRepository
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class FavoriteRepositoryAdapter(
    private val jpaRepository: FavoriteJpaRepository,
) : FavoriteRepository {

    override fun findByUser(userId: UUID): List<Favorite> =
        jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).map { it.toDomain() }

    override fun findIdsByUser(userId: UUID): Set<UUID> =
        jpaRepository.findByUserId(userId).map { it.publicationId }.toSet()

    override fun existsByUserAndPublication(userId: UUID, publicationId: UUID): Boolean =
        jpaRepository.existsByUserIdAndPublicationId(userId, publicationId)

    override fun save(favorite: Favorite) {
        jpaRepository.save(favorite.toEntity())
    }

    @Transactional
    override fun delete(userId: UUID, publicationId: UUID) {
        jpaRepository.deleteByUserIdAndPublicationId(userId, publicationId)
    }

    private fun FavoriteEntity.toDomain(): Favorite =
        Favorite(id = id, userId = userId, publicationId = publicationId, createdAt = createdAt)

    private fun Favorite.toEntity(): FavoriteEntity =
        FavoriteEntity(id = id, userId = userId, publicationId = publicationId, createdAt = createdAt)
}
