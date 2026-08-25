package com.coliving.api.identity.application.usecase

import com.coliving.api.identity.application.dto.RequestPasswordResetCommand
import com.coliving.api.identity.application.port.out.EmailSender
import com.coliving.api.identity.application.port.out.TokenGenerator
import com.coliving.api.identity.domain.enums.SecurityTokenPurpose
import com.coliving.api.identity.domain.model.SecurityToken
import com.coliving.api.identity.domain.repository.SecurityTokenStore
import com.coliving.api.identity.domain.repository.UserRepository
import com.coliving.api.identity.domain.vo.Email
import com.coliving.api.identity.infrastructure.config.IdentitySecurityProperties
import com.coliving.api.shared.error.InvalidArgumentException
import java.time.Clock
import org.springframework.stereotype.Service

/**
 * RF-003: request a password reset. The response is always a success even when
 * the e-mail is unknown, to avoid account enumeration.
 */
@Service
class RequestPasswordResetService(
    private val userRepository: UserRepository,
    private val securityTokenStore: SecurityTokenStore,
    private val tokenGenerator: TokenGenerator,
    private val emailSender: EmailSender,
    private val properties: IdentitySecurityProperties,
    private val clock: Clock,
) {

    fun request(command: RequestPasswordResetCommand) {
        val email = try {
            Email.of(command.email)
        } catch (e: IllegalArgumentException) {
            throw InvalidArgumentException("Invalid email address")
        }

        val user = userRepository.findByEmail(email)
        if (user == null || !user.isActive()) {
            // Do not disclose whether the account exists.
            return
        }

        val now = clock.instant()
        val raw = tokenGenerator.generate()
        val token = SecurityToken.create(
            userId = user.id,
            purpose = SecurityTokenPurpose.PASSWORD_RESET,
            tokenHash = raw.hash,
            now = now,
            ttl = properties.passwordResetTokenTtl,
        )
        securityTokenStore.save(token)
        emailSender.sendPasswordResetEmail(email, raw.raw)
    }
}