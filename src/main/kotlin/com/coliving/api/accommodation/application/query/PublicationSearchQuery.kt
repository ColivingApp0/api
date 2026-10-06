package com.coliving.api.accommodation.application.query

import com.coliving.api.accommodation.application.dto.PublishedListingInfo
import java.util.UUID

/** Read contract consumed by the `search` context (RF-030, RF-034). */
interface PublicationSearchQuery {

    /** All PUBLICADA room listings with their public monthly facets. */
    fun findPublishedListings(): List<PublishedListingInfo>

    /** Whether the publication exists and is currently PUBLICADA. */
    fun isPublished(publicationId: UUID): Boolean
}
