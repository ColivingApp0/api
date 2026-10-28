package com.coliving.api.accommodation.domain.model

import java.time.Instant
import java.util.UUID

data class PropertyImage(
    val id: UUID,
    val propertyId: UUID,
    val storageKey: String,
    val originalFilename: String,
    val contentType: String,
    val sizeBytes: Long,
    val position: Int,
    val createdAt: Instant,
)
