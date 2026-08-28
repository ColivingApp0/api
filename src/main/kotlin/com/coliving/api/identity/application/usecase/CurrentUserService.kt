package com.coliving.api.identity.application.usecase

import com.coliving.api.identity.application.dto.ConsentSummary
import com.coliving.api.identity.application.dto.CurrentUserResult
import com.coliving.api.identity.application.dto.DocumentSummary
import com.coliving.api.identity.application.dto.UserRoleSummary
import com.coliving.api.identity.domain.model.Consent
import com.coliving.api.identity.domain.model.VerificationDocument
import com.coliving.api.identity.domain.repository.ConsentRepository
import com.coliving.api.identity.domain.repository.RoleRepository
import com.coliving.api.identity.domain.repository.VerificationDocumentRepository
import com.coliving.api.identity.domain.repository.UserRepository
import com.coliving.api.shared.error.NotFoundException
import java.util.UUID
import org.springframework.stereotype.Service

/**
 * Query use case: assembles the current user's identity view (account, roles,
 * consents and verification documents). Profile data is served by the `profile`
 * bounded context; the verification level is derived by UserVerificationQueryService.
 */
@Service
class CurrentUserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val consentRepository: ConsentRepository,
    private val verificationDocumentRepository: VerificationDocumentRepository,
) {

    fun current(userId: UUID): CurrentUserResult {
        val user = userRepository.findById(userId)
            ?: throw NotFoundException("User not found")
        val roleIndex = roleRepository.findAllActive().associateBy { it.code }

        val roles = user.roles.map { ur ->
            UserRoleSummary(
                id = ur.id,
                roleId = ur.roleId,
                code = ur.roleCode,
                name = roleIndex[ur.roleCode]?.name ?: ur.roleCode.name,
                active = ur.isActive,
                assignedAt = ur.assignedAt,
            )
        }
        val activeRole = roles.firstOrNull { it.active }
        val consents = consentRepository.findByUser(user.id)
        val documents = verificationDocumentRepository.findByUser(user.id)

        return CurrentUserResult(
            userId = user.id,
            email = user.email.value,
            emailVerified = user.emailVerified,
            status = user.status,
            roles = roles,
            activeRole = activeRole,
            consents = consents.map { it.toSummary() },
            documents = documents.map { it.toSummary() },
        )
    }

    private fun Consent.toSummary(): ConsentSummary =
        ConsentSummary(
            type = type,
            version = version,
            purpose = purpose,
            accepted = accepted,
            timestamp = timestamp,
        )

    private fun VerificationDocument.toSummary(): DocumentSummary =
        DocumentSummary(
            id = id,
            type = type,
            status = status,
            uploadedAt = uploadedAt,
            reviewedAt = reviewedAt,
            reviewReason = reviewReason,
        )
}