package com.coliving.api.shared.presentation

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.annotation.AuthenticationPrincipal

/**
 * OpenAPI 3 document and Swagger UI of the whole HTTP surface.
 *
 * Swagger UI lives at `/swagger-ui/index.html` (shortcut `/swagger-ui.html`) and
 * the document at `/v3/api-docs` (also `/v3/api-docs.yaml`). Both paths are
 * public in [com.coliving.api.identity.infrastructure.security.SecurityConfig]
 * and are switched off in production unless the deployment opts in.
 *
 * The document is derived from the controllers: every route, parameter and
 * request/response body comes from the code, so there is no hand-written list to
 * keep in sync. Controllers are grouped with `@Tag`.
 */
@Configuration
class OpenApiConfig {

    init {
        // The principal is injected by Spring Security (@AuthenticationPrincipal)
        // and never travels in the request: without this the `current` argument
        // would be documented as a bogus query/body parameter of every
        // authenticated operation.
        SpringDocUtils.getConfig().addAnnotationsToIgnore(AuthenticationPrincipal::class.java)
    }

    @Bean
    fun colivingOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("ColivinApp API")
                .version(API_VERSION)
                .description(
                    """
                    REST API of the ColivinApp coliving pilot: a modular monolith documented
                    module by module (auth, profile, accommodation, bookings, search, messaging,
                    community, moderation and reputation). Every body is JSON and `RF-xxx`
                    references point to the functional requirements of the project's SRS.

                    Every operation requires the session bearer token returned by
                    `POST /api/v1/auth/login` (`Authorization: Bearer <token>`, one token per
                    device, revoked by logout [RF-002]), except the public authentication flows.
                    The `/api/v1/admin/**` namespace additionally requires the MODERADOR or
                    ADMINISTRADOR role.
                    """.trimIndent(),
                ),
        )
        .components(
            Components().addSecuritySchemes(BEARER_SCHEME, bearerSessionScheme()),
        )
        // Global requirement, mirroring SecurityConfig's `anyRequest().authenticated()`;
        // the public authentication operations opt out with @SecurityRequirements.
        .addSecurityItem(SecurityRequirement().addList(BEARER_SCHEME))

    /**
     * The token is an opaque session token issued by the identity context, not a
     * JWT, so only the HTTP bearer scheme is declared (no `bearerFormat`).
     */
    private fun bearerSessionScheme(): SecurityScheme = SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .description("Session token returned by POST /api/v1/auth/login or POST /api/v1/auth/register.")

    companion object {
        /** Name of the security scheme, shared with the controllers' annotations. */
        const val BEARER_SCHEME = "bearerAuth"

        /** Version of the documented API, matching the `/api/v1` route prefix. */
        const val API_VERSION = "v1"
    }
}