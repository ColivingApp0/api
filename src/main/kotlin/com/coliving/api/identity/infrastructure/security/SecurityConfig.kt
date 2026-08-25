package com.coliving.api.identity.infrastructure.security

import jakarta.servlet.http.HttpServletResponse
import java.nio.charset.StandardCharsets
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import tools.jackson.databind.ObjectMapper

// Stateless bearer-token security for the modular backend (RF-002 / RNF-002).
// Public endpoints: authentication flows (register, login, verify email,
// password reset). The admin namespace additionally requires an internal role;
// everything else demands a valid session.
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val bearerTokenAuthenticationFilter: BearerTokenAuthenticationFilter,
    private val objectMapper: ObjectMapper,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests {
                it
                    .requestMatchers(
                        "/api/v1/auth/login",
                        "/api/v1/auth/register",
                        "/api/v1/auth/verify-email",
                        "/api/v1/auth/password-reset/request",
                        "/api/v1/auth/password-reset",
                        "/error",
                    ).permitAll()
                    .requestMatchers("/api/v1/admin/**")
                    .hasAnyRole("MODERADOR", "ADMINISTRADOR")
                    .anyRequest()
                    .authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, _ ->
                    writeError(response, HttpStatus.UNAUTHORIZED, "Authentication required")
                }
                it.accessDeniedHandler { _, response, _ ->
                    writeError(response, HttpStatus.FORBIDDEN, "Insufficient privileges")
                }
            }
            .addFilterBefore(
                bearerTokenAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java,
            )
        return http.build()
    }

    private fun writeError(
        response: HttpServletResponse,
        status: HttpStatus,
        message: String,
    ) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = StandardCharsets.UTF_8.name()
        response.writer.write(
            objectMapper.writeValueAsString(mapOf("message" to message)),
        )
    }
}