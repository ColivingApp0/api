package com.coliving.api.accommodation.infrastructure.provider

import com.coliving.api.accommodation.application.port.out.PublicProfileCard
import com.coliving.api.accommodation.application.port.out.PublicProfilePort
import com.coliving.api.profile.application.query.PublicProfileQuery
import java.util.UUID
import org.springframework.stereotype.Component

/** Adapter over profile's PublicProfileQuery. */
@Component
class ProfilePublicCardProvider(
    private val publicProfileQuery: PublicProfileQuery,
) : PublicProfilePort {

    override fun findPublicProfile(userId: UUID): PublicProfileCard? =
        publicProfileQuery.cardOf(userId)?.let {
            PublicProfileCard(
                userId = it.userId,
                name = it.name,
                avatarUrl = it.avatarUrl,
                roommateProfileVisible = it.roommateProfileVisible,
            )
        }
}
