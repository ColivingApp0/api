package com.coliving.api.identity.application.usecase

import com.coliving.api.identity.application.dto.LoginCommand
import com.coliving.api.identity.application.dto.LoginResult
import com.coliving.api.identity.application.dto.LoginUserSummary
import com.coliving.api.identity.application.dto.RoleSummary
import com.coliving.api.identity.application.port.out.PasswordHasher
import com.coliving.api.identity.application.port.out.TokenGenerator
import com.coliving.api.identity.domain.model.SessionToken
import com.coliving.api.identity.domain.repository.RoleRepository
import com.coliving.api.identity.domain.repository.SessionTokenStore
import com.coliving.api.identity.domain.repository.UserRepository
import com.coliving.api.identity.domain.vo.Email
import com.coliving.api.identity.infrastructure.config.IdentitySecurityProperties
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.InvalidArgumentException
import com.coliving.api.shared.error.UnauthorizedException
import java.time.Clock
import org.springframework.stereotype.Service

/**
 * RF-002: valid credentials produce a session. A new opaque token is issued per
 * device, so web and mobile sessions coexist and can be revoked independently.
 */
@Service
class AuthenticateUserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordHasher: PasswordHasher,
    private val sessionTokenStore: SessionTokenStore,
    private val tokenGenerator: TokenGenerator,
    private val properties: IdentitySecurityProperties,
    private val clock: Clock,
) {

    fun login(command: LoginCommand): LoginResult {
        require(command.email.isNotBlank() && command.password.isNotBlank()) {
            throw InvalidArgumentException("Email and password are required")
        }
        val email = try {
            Email.of(command.email)
        } catch (e: IllegalArgumentException) {
            throw UnauthorizedException("Invalid credentials")
        }

        val user = userRepository.findByEmail(email)
            ?: throw UnauthorizedException("Invalid credentials")
        if (!passwordHasher.matches(command.password, user.passwordHash.value)) {
            throw UnauthorizedException("Invalid credentials")
        }
        if (!user.isActive()) {
            throw ForbiddenException("Account is not active")
        }

        val now = clock.instant()
        val raw = tokenGenerator.generate()
        val session = SessionToken.create(
            userId = user.id,
            tokenHash = raw.hash,
            deviceLabel = command.deviceLabel.ifBlank { "unknown" },
            now = now,
            ttl = properties.sessionTtl,
        )
        sessionTokenStore.save(session)

        val roleIndex = roleRepository.findAllActive().associateBy { it.code }
        val activeRoles = user.activeRoleCodes().mapNotNull { roleIndex[it] }
            .map { RoleSummary(id = it.id, code = it.code, name = it.name) }

        return LoginResult(
            token = raw.raw,
            expiresAt = session.expiresAt,
            user = LoginUserSummary(
                userId = user.id,
                email = user.email.value,
                emailVerified = user.emailVerified,
                status = user.status,
                activeRoles = activeRoles,
            ),
        )
    }
}