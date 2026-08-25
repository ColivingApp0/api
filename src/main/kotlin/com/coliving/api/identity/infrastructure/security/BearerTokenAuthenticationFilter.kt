package com.coliving.api.identity.infrastructure.security

import com.coliving.api.identity.application.port.out.TokenGenerator
import com.coliving.api.identity.domain.repository.SessionTokenStore
import com.coliving.api.identity.domain.repository.UserRepository
import com.coliving.api.shared.security.CurrentUser
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.time.Clock
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Validates `Authorization: Bearer <token>` against the session store (hash
 * lookup). A valid, non-expired, non-revoked session for an active user becomes
 * the request's authentication. Unauthenticated requests simply continue and
 * are rejected later by the authorization rules.
 */
@Component
class BearerTokenAuthenticationFilter(
    private val sessionTokenStore: SessionTokenStore,
    private val userRepository: UserRepository,
    private val tokenGenerator: TokenGenerator,
    private val clock: Clock,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (header != null && header.startsWith("Bearer ")) {
            val raw = header.removePrefix("Bearer ").trim()
            if (raw.isNotEmpty()) {
                authenticate(raw)
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun authenticate(rawToken: String) {
        val session = sessionTokenStore.findByHash(tokenGenerator.hash(rawToken))
            ?: return
        val now = clock.instant()
        if (!session.isActive(now)) return

        val user = userRepository.findById(session.userId) ?: return
        if (!user.isActive()) return

        val principal = CurrentUser(
            userId = user.id,
            email = user.email.value,
            roleCodes = user.activeRoleCodes().map { it.name }.toSet(),
        )
        val authentication = UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.authorities(),
        )
        SecurityContextHolder.getContext().authentication = authentication
    }
}