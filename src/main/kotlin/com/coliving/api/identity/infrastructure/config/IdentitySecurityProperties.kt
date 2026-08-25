package com.coliving.api.identity.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Security tuning for the identity module, bound from `identity.security.*`.
 * Defaults match the pilot assumptions and can be overridden per environment.
 */
@ConfigurationProperties(prefix = "identity.security")
data class IdentitySecurityProperties(
    /** Lifetime of an opaque session token (RF-002). */
    val sessionTtl: Duration = Duration.ofDays(30),
    /** Lifetime of a single-use email verification token (RF-001). */
    val emailVerificationTokenTtl: Duration = Duration.ofHours(24),
    /** Lifetime of a single-use password reset token (RF-003). */
    val passwordResetTokenTtl: Duration = Duration.ofMinutes(30),
)