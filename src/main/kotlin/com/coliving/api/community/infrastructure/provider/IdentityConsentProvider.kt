package com.coliving.api.community.infrastructure.provider

import com.coliving.api.community.application.port.out.CommunityConsentPort
import com.coliving.api.identity.application.query.UserConsentQuery
import com.coliving.api.identity.domain.enums.ConsentType
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Adapter over identity's [UserConsentQuery] (RF-062 / RN-05): the aggregated
 * community summary counts only residents who granted the privacy consent, so
 * revoking it immediately removes them from the aggregate.
 */
@Component
class IdentityConsentProvider(
    private val userConsentQuery: UserConsentQuery,
) : CommunityConsentPort {

    override fun hasDataSharingConsent(userId: UUID): Boolean =
        userConsentQuery.hasAccepted(userId, ConsentType.PRIVACIDAD)
}