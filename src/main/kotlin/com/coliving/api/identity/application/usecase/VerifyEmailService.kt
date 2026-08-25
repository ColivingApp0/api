package com.coliving.api.identity.application.usecase

import com.coliving.api.identity.application.dto.VerifyEmailCommand
import com.coliving.api.identity.application.port.out.TokenGenerator
import com.coliving.api.identity.domain.enums.SecurityTokenPurpose
import com.coliving.api.identity.domain.repository.SecurityTokenStore
import com.coliving.api.identity.domain.repository.UserRepository
import com.coliving.api.shared.error.InvalidArgumentException
import com.coliving.api.shared.error.NotFoundException
import java.time.Clock
import org.springframework.stereotype.Service

/**
 * RF-001: the user confirms the e-mail through the single-use link/code and the
 * account becomes verified. The token is consumed atomically (single use).
 */
@Service
class VerifyEmailService(
    private val securityTokenStore: SecurityTokenStore,
    private val userRepository: UserRepository,
    private val tokenGenerator: TokenGenerator,
    private val clock: Clock,
) {

    fun verify(command: VerifyEmailCommand) {
        if (command.token.isBlank()) throw InvalidArgumentException("Token is required")
        val token = securityTokenStore.findByHashAndPurpose(
            tokenGenerator.hash(command.token),
            SecurityTokenPurpose.EMAIL_VERIFICATION,
        ) ?: throw InvalidArgumentException("Invalid or expired verification token")

        val now = clock.instant()
        token.consume(now)
        securityTokenStore.markUsed(token.id, now)

        val user = userRepository.findById(token.userId)
            ?: throw NotFoundException("User not found")
        user.verifyEmail()
        userRepository.save(user)
    }
}