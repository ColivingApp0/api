package com.coliving.api.identity.infrastructure.security

import com.coliving.api.identity.domain.enums.RoleCode
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.UUID

/**
 * Authenticated principal used across controllers and filters. It carries only
 * what authorization needs: the user id, the e-mail and the active roles.
 */
data class CurrentUser(
    val userId: UUID,
    val email: String,
    val activeRoles: Set<RoleCode>,
) {
    fun authorities(): List<GrantedAuthority> =
        activeRoles.map { SimpleGrantedAuthority("ROLE_${it.name}") }
}