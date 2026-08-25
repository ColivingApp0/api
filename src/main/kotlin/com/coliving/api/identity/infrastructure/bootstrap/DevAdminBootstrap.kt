package com.coliving.api.identity.infrastructure.bootstrap

import com.coliving.api.identity.application.port.out.PasswordHasher
import com.coliving.api.identity.domain.enums.RoleCode
import com.coliving.api.identity.domain.model.User
import com.coliving.api.identity.domain.repository.RoleRepository
import com.coliving.api.identity.domain.repository.UserRepository
import com.coliving.api.identity.domain.vo.Email
import com.coliving.api.identity.domain.vo.PasswordHash
import java.time.Clock
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Dev-only bootstrap that creates the first ADMINISTRADOR account.
 * Enabled through `identity.bootstrap.enabled` (default `false`; set in the
 * `dev` profile). This solves the chicken-and-egg problem of seeding an admin
 * without hard-coding BCrypt strings in migrations.
 */
@Component
class DevAdminBootstrap(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordHasher: PasswordHasher,
    private val clock: Clock,
    @Value("\${identity.bootstrap.enabled:false}") private val enabled: Boolean,
    @Value("\${identity.bootstrap.admin-email:}") private val adminEmail: String,
    @Value("\${identity.bootstrap.admin-password:}") private val adminPassword: String,
) {

    private val logger = LoggerFactory.getLogger(DevAdminBootstrap::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun bootstrap() {
        if (!enabled) return
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            logger.warn("identity.bootstrap.enabled=true but admin email/password are blank; skipping")
            return
        }

        val email = Email.of(adminEmail)
        if (userRepository.existsByEmail(email)) return

        val now: Instant = clock.instant()
        val user = User.register(email, PasswordHash.of(passwordHasher.hash(adminPassword)), now)
        user.verifyEmail()

        val adminRole = roleRepository.findByCode(RoleCode.ADMINISTRADOR)
            ?: throw IllegalStateException("Role ADMINISTRADOR is missing from the catalog")
        user.assignRole(adminRole, now)
        userRepository.save(user)

        logger.info("Bootstrap admin account created: {}", email.value)
    }
}