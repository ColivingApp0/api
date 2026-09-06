package com.coliving.api.accommodation.application.query

import com.coliving.api.accommodation.application.dto.PublishedListingInfo
import java.util.UUID

/**
 * Read contract consumed by the `search` context (RF-030, RF-034): published
 * listings with their public facets, and a publication-visibility check used
 * when a guest bookmarks a listing as favorite.
 */
interface PublicationSearchQuery {

    /** All PUBLICADA listings with their public pricing/city facets. */
    fun findPublishedListings(): List<PublishedListingInfo>

    /** Whether the publication exists and is currently PUBLICADA. */
    fun isPublished(publicationId: UUID): Boolean
}
