package com.coliving.api.shared.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.UUID

/**
 * Authenticated principal shared by every bounded context. It carries only what
 * authorization needs: the user id, the e-mail and the active role codes as
 * plain strings, so `shared` never depends on a context's domain enums.
 */
data class CurrentUser(
    val userId: UUID,
    val email: String,
    val roleCodes: Set<String>,
) {
    fun authorities(): List<GrantedAuthority> =
        roleCodes.map { SimpleGrantedAuthority("ROLE_$it") }
}