package com.coliving.api.identity.application.port.out

/**
 * Out port that produces high-entropy opaque tokens (sessions, email
 * verification, password reset) and their one-way hash.
 *
 * The [RawToken.raw] value is handed to the client exactly once; only
 * [RawToken.hash] is persisted.
 */
interface TokenGenerator {
    fun generate(): RawToken
    fun hash(rawToken: String): String
}

data class RawToken(
    val raw: String,
    val hash: String,
)