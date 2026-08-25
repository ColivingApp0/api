package com.coliving.api.identity.infrastructure.security

import com.coliving.api.identity.application.port.out.PasswordHasher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * BCrypt-based implementation of [PasswordHasher] (RNF-001: no plain-text secrets).
 */
@Component
class CryptoPasswordHasher(
    private val passwordEncoder: PasswordEncoder,
) : PasswordHasher {

    override fun hash(raw: String): String {
        // Spring Security's PasswordEncoder encodes to a non-null string;
        // the elvis guard keeps the platform type honest.
        return passwordEncoder.encode(raw) ?: error("Password encoder returned null")
    }

    override fun matches(raw: String, hash: String): Boolean =
        passwordEncoder.matches(raw, hash)
}