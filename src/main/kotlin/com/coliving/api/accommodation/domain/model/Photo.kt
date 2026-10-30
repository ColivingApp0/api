package com.coliving.api.accommodation.domain.model

import java.util.UUID

/** Photograph belonging to a property (building/common areas). */
data class PropertyPhoto(
    val id: UUID,
    val propertyId: UUID,
    val url: String,
    val position: Int,
)

/** Photograph belonging to a specific room. */
data class UnitPhoto(
    val id: UUID,
    val unitId: UUID,
    val url: String,
    val position: Int,
)
