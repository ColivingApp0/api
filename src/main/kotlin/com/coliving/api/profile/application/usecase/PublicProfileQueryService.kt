package com.coliving.api.profile.application.usecase

import com.coliving.api.profile.application.dto.PublicProfileCard
import com.coliving.api.profile.application.query.PublicProfileQuery
import com.coliving.api.profile.domain.repository.ProfileRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PublicProfileQueryService(
    private val profileRepository: ProfileRepository,
) : PublicProfileQuery {

    @Transactional(readOnly = true)
    override fun cardOf(userId: UUID): PublicProfileCard? =
        profileRepository.findByUserId(userId)?.let {
            PublicProfileCard(
                userId = it.userId,
                name = it.name,
                avatarUrl = it.avatarUrl,
                roommateProfileVisible = it.roommateProfileVisible,
            )
        }
}
