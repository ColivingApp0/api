package com.coliving.api.profile.application.query

import com.coliving.api.profile.application.dto.PublicProfileCard
import java.util.UUID

/**
 * Internal read contract: minimal public profile card (name/avatar + explicit
 * roommate-sharing consent). Consumed by accommodation's roommate view.
 */
interface PublicProfileQuery {
    fun cardOf(userId: UUID): PublicProfileCard?
}
