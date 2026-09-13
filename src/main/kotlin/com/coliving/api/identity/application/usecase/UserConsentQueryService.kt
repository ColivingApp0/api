package com.coliving.api.identity.application.usecase

import com.coliving.api.identity.application.query.UserConsentQuery
import com.coliving.api.identity.domain.enums.ConsentType
import com.coliving.api.identity.domain.repository.ConsentRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read-side implementation of [UserConsentQuery]. A consent counts only while
 * it is accepted: revoking it (RF-005) immediately hides the user from the
 * consumers of this projection.
 */
@Service
class UserConsentQueryService(
    private val consentRepository: ConsentRepository,
) : UserConsentQuery {

    @Transactional(readOnly = true)
    override fun hasAccepted(userId: UUID, type: ConsentType): Boolean =
        consentRepository.findByUserAndType(userId, type)?.accepted == true
}