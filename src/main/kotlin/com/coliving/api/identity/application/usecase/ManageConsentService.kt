package com.coliving.api.identity.application.usecase

import com.coliving.api.identity.application.dto.ConsentSummary
import com.coliving.api.identity.application.dto.ManageConsentCommand
import com.coliving.api.identity.domain.model.Consent
import com.coliving.api.identity.domain.repository.ConsentRepository
import com.coliving.api.shared.error.InvalidArgumentException
import com.coliving.api.shared.error.NotFoundException
import java.time.Clock
import org.springframework.stereotype.Service

/**
 * RF-005: records acceptance of terms / privacy policy / specific authorizations.
 * Version, date, user and purpose are always preserved (accept/revoke history).
 */
@Service
class ManageConsentService(
    private val consentRepository: ConsentRepository,
    private val clock: Clock,
) {

    fun manage(command: ManageConsentCommand): ConsentSummary {
        val version = command.version.trim()
        val purpose = command.purpose.trim()
        if (version.isEmpty() || purpose.isEmpty()) {
            throw InvalidArgumentException("Version and purpose are required")
        }

        val now = clock.instant()
        val consent = consentRepository.findByUserAndType(command.userId, command.type)
        if (consent == null) {
            if (!command.accept) throw NotFoundException("Consent was never accepted")
            val created = Consent.create(command.userId, command.type, version, purpose, now)
            consentRepository.save(created)
            return consentRepository.findByUserAndType(command.userId, command.type)!!
                .let { it.toSummary() }
        }

        if (command.accept) consent.accept(version, purpose, now) else consent.revoke(now)
        consentRepository.save(consent)
        return consent.toSummary()
    }

    private fun Consent.toSummary(): ConsentSummary =
        ConsentSummary(
            type = type,
            version = version,
            purpose = purpose,
            accepted = accepted,
            timestamp = timestamp,
        )
}