package com.coliving.api.search.infrastructure.persistence.repository

import com.coliving.api.search.infrastructure.persistence.entity.FavoriteEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface FavoriteJpaRepository : JpaRepository<FavoriteEntity, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<FavoriteEntity>
    fun findByUserId(userId: UUID): List<FavoriteEntity>
    fun existsByUserIdAndPublicationId(userId: UUID, publicationId: UUID): Boolean
    fun deleteByUserIdAndPublicationId(userId: UUID, publicationId: UUID): Long
}
