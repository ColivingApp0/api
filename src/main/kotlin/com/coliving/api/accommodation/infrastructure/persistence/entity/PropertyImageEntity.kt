package com.coliving.api.accommodation.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "accommodation_property_image")
class PropertyImageEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "property_id", nullable = false)
    var propertyId: UUID,

    @Column(name = "storage_key", nullable = false, length = 512)
    var storageKey: String,

    @Column(name = "original_filename", nullable = false, length = 255)
    var originalFilename: String,

    @Column(name = "content_type", nullable = false, length = 100)
    var contentType: String,

    @Column(name = "size_bytes", nullable = false)
    var sizeBytes: Long,

    @Column(name = "sort_order", nullable = false)
    var position: Int,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
)
