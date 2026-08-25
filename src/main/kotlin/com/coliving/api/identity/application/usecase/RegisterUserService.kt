package com.coliving.api.identity.application.usecase

import com.coliving.api.identity.application.dto.RegisterUserCommand
import com.coliving.api.identity.application.dto.RegisterUserResult
import com.coliving.api.identity.application.port.out.EmailSender
import com.coliving.api.identity.application.port.out.PasswordHasher
import com.coliving.api.identity.application.port.out.TokenGenerator
import com.coliving.api.identity.domain.model.SecurityToken
import com.coliving.api.identity.domain.model.User
import com.coliving.api.identity.domain.enums.SecurityTokenPurpose
import com.coliving.api.identity.domain.repository.SecurityTokenStore
import com.coliving.api.identity.domain.repository.UserRepository
import com.coliving.api.identity.domain.vo.Email
import com.coliving.api.identity.domain.vo.PasswordHash
import com.coliving.api.identity.infrastructure.config.IdentitySecurityProperties
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.InvalidArgumentException
import java.time.Clock
import org.springframework.stereotype.Service

/**
 * RF-001: create an account with email + password, verify ownership of the
 * email before sensitive operations are enabled. A one-time verification token
 * is persisted (hash only) and the delivery is delegated to [EmailSender].
 */
@Service
class RegisterUserService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val securityTokenStore: SecurityTokenStore,
    private val tokenGenerator: TokenGenerator,
    private val emailSender: EmailSender,
    private val properties: IdentitySecurityProperties,
    private val clock: Clock,
) {

    fun register(command: RegisterUserCommand): RegisterUserResult {
        val email = try {
            Email.of(command.email)
        } catch (e: IllegalArgumentException) {
            throw InvalidArgumentException("Invalid email address")
        }
        requireValidPassword(command.password)
        if (userRepository.existsByEmail(email)) {
            throw ConflictException("Email already registered")
        }

        val now = clock.instant()
        val user = User.register(email, PasswordHash.of(passwordHasher.hash(command.password)), now)
        userRepository.save(user)

        val raw = tokenGenerator.generate()
        val verificationToken = SecurityToken.create(
            userId = user.id,
            purpose = SecurityTokenPurpose.EMAIL_VERIFICATION,
            tokenHash = raw.hash,
            now = now,
            ttl = properties.emailVerificationTokenTtl,
        )
        securityTokenStore.save(verificationToken)
        emailSender.sendVerificationEmail(email, raw.raw)

        return RegisterUserResult(
            userId = user.id,
            status = user.status,
            emailVerified = user.emailVerified,
        )
    }

    private fun requireValidPassword(password: String) {
        if (password.length < 8 || password.length > 72) {
            throw InvalidArgumentException("Password must be between 8 and 72 characters")
        }
    }
}