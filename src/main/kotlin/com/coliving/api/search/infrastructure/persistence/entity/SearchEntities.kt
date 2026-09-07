package com.coliving.api.search.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "search_favorite")
class FavoriteEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    // Cross-context reference to accommodation_publication (no FK by rule).
    @Column(name = "publication_id", nullable = false)
    var publicationId: UUID,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
)
