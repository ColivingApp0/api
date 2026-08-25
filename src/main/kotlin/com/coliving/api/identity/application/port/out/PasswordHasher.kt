package com.coliving.api.identity.application.port.out

/**
 * Out port for password hashing (RNF-001: credentials and secrets are never
 * stored in plain text). Implemented with BCrypt in infrastructure.
 */
interface PasswordHasher {
    fun hash(raw: String): String
    fun matches(raw: String, hash: String): Boolean
}