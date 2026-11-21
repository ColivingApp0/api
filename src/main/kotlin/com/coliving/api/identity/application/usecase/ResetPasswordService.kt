package com.coliving.api.identity.application.usecase

import com.coliving.api.identity.application.dto.ResetPasswordCommand
import com.coliving.api.identity.application.port.out.PasswordHasher
import com.coliving.api.identity.application.port.out.TokenGenerator
import com.coliving.api.identity.domain.enums.SecurityTokenPurpose
import com.coliving.api.identity.domain.enums.UserStatus
import com.coliving.api.identity.domain.repository.SecurityTokenStore
import com.coliving.api.identity.domain.repository.UserRepository
import com.coliving.api.identity.domain.vo.PasswordHash
import com.coliving.api.shared.error.InvalidArgumentException
import com.coliving.api.shared.error.NotFoundException
import java.time.Clock
import org.springframework.stereotype.Service

/**
 * RF-003: consuming a valid, single-use reset token establishes a new password.
 *
 * A successful reset also proves ownership of the mailbox (the token is only
 * delivered there), so an account still in [UserStatus.PENDING_EMAIL_VERIFICATION]
 * is verified and promoted to ACTIVE. Without this, a user that never clicked
 * the original verification link could reset the password but still be unable
 * to log in. This does not affect SUSPENDED accounts: `verifyEmail()` only
 * promotes the pending state and never reactivates a suspended account.
 */
@Service
class ResetPasswordService(
    private val securityTokenStore: SecurityTokenStore,
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val tokenGenerator: TokenGenerator,
    private val clock: Clock,
) {

    fun reset(command: ResetPasswordCommand) {
        validatePassword(command.newPassword)
        if (command.token.isBlank()) throw InvalidArgumentException("Token is required")

        val token = securityTokenStore.findByHashAndPurpose(
            tokenGenerator.hash(command.token),
            SecurityTokenPurpose.PASSWORD_RESET,
        ) ?: throw InvalidArgumentException("Invalid or expired reset token")

        val now = clock.instant()
        token.consume(now)
        securityTokenStore.markUsed(token.id, now)

        val user = userRepository.findById(token.userId)
            ?: throw NotFoundException("User not found")
        user.updatePassword(PasswordHash.of(passwordHasher.hash(command.newPassword)))
        user.verifyEmail()
        userRepository.save(user)
    }

    private fun validatePassword(password: String) {
        if (password.length < 8 || password.length > 72) {
            throw InvalidArgumentException("Password must be between 8 and 72 characters")
        }
    }
}