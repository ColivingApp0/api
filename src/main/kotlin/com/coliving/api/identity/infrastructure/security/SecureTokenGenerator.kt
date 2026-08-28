package com.coliving.api.identity.infrastructure.security

import com.coliving.api.identity.application.port.out.RawToken
import com.coliving.api.identity.application.port.out.TokenGenerator
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import org.springframework.stereotype.Component

/**
 * Generates 256-bit random opaque tokens (Base64URL) and exposes their SHA-256
 * hash. Only the hash is stored, so a database leak does not expose usable
 * tokens (supports RF-002 / RF-003 design).
 */
@Component
class SecureTokenGenerator : TokenGenerator {

    private val random = SecureRandom()

    override fun generate(): RawToken {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        return RawToken(raw = raw, hash = hash(raw))
    }

    override fun hash(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawToken.toByteArray(StandardCharsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}