package com.coliving.api.identity.application.usecase

import com.coliving.api.identity.application.query.UserVerificationQuery
import com.coliving.api.identity.domain.enums.DocumentStatus
import com.coliving.api.identity.domain.enums.VerificationLevel
import com.coliving.api.identity.domain.repository.VerificationDocumentRepository
import java.util.UUID
import org.springframework.stereotype.Service

/**
 * Derived verification level (RF-014): COMPLETO when at least one verification
 * document has been approved; otherwise NO_VERIFICADO. BASICO remains reserved
 * for future flows. This replaces the previously stored `Profile.verificationLevel`.
 */
@Service
class UserVerificationQueryService(
    private val verificationDocumentRepository: VerificationDocumentRepository,
) : UserVerificationQuery {

    override fun levelOf(userId: UUID): VerificationLevel {
        val hasApprovedDocument = verificationDocumentRepository.findByUser(userId)
            .any { it.status == DocumentStatus.APROBADO }
        return if (hasApprovedDocument) VerificationLevel.COMPLETO else VerificationLevel.NO_VERIFICADO
    }
}