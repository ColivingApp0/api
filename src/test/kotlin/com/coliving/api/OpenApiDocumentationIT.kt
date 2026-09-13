package com.coliving.api

import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

/**
 * Guards the OpenAPI document behind Swagger UI: it must list every route the
 * controllers expose (RF-001..RF-084), declare the bearer session scheme, keep
 * the public authentication flows anonymous and never document the injected
 * principal as a request parameter. The full context runs against the ephemeral
 * PostgreSQL container, so the security filter chain is the one that decides the
 * access to the documentation endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationIT : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `the document lists every route of the API`() {
        assertEquals(
            EXPECTED_OPERATIONS.sorted(),
            documentedOperations().sorted(),
            "Every controller route must appear in the OpenAPI document",
        )
    }

    @Test
    fun `the controllers are grouped by tag`() {
        val documented = (document()["tags"] as List<*>)
            .map { (it as Map<*, *>)["name"].toString() }
        assertEquals(EXPECTED_TAGS.sorted(), documented.sorted())
    }

    @Test
    fun `the document describes the API and its bearer session scheme`() {
        val document = document()
        val info = document["info"] as Map<*, *>
        assertEquals("ColivinApp API", info["title"])
        assertEquals("v1", info["version"])

        val schemes = (document["components"] as Map<*, *>)["securitySchemes"] as Map<*, *>
        val bearer = schemes["bearerAuth"] as Map<*, *>
        assertEquals("http", bearer["type"])
        assertEquals("bearer", bearer["scheme"])

        // Global requirement: every operation needs the token unless it opts out.
        assertEquals(listOf(mapOf("bearerAuth" to emptyList<String>())), document["security"])
    }

    @Test
    fun `public authentication flows are documented as anonymous`() {
        val paths = paths()
        PUBLIC_AUTH_OPERATIONS.forEach { operation ->
            val (method, path) = operation.split(" ", limit = 2)
            assertEquals(
                emptyList<Any>(),
                operation(paths, method, path)["security"],
                "$operation must be documented without the bearer requirement",
            )
        }
        // Logout is authenticated, so it inherits the global requirement.
        assertTrue(operation(paths, "POST", "/api/v1/auth/logout")["security"] == null)
    }

    @Test
    fun `the injected principal is not documented as a request parameter`() {
        // @AuthenticationPrincipal arguments are injected by Spring Security, so
        // they must never show up as query or body parameters.
        val paths = paths()
        assertTrue(operation(paths, "GET", "/api/v1/profile")["parameters"] == null)
        assertTrue(operation(paths, "GET", "/api/v1/notifications")["parameters"] == null)
        assertTrue(operation(paths, "GET", "/api/v1/favorites")["parameters"] == null)
    }

    @Test
    fun `the documented endpoints are reachable without a token`() {
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection)
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk)
        mockMvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk)
    }

    private fun document(): Map<*, *> {
        val json = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .getContentAsString(StandardCharsets.UTF_8)
        return objectMapper.readValue(json, Map::class.java)
    }

    private fun paths(): Map<*, *> = document()["paths"] as Map<*, *>

    private fun operation(paths: Map<*, *>, method: String, path: String): Map<*, *> {
        val item = paths[path] as Map<*, *>? ?: error("$path is missing from the document")
        return item[method.lowercase()] as Map<*, *>?
            ?: error("$method $path is missing from the document")
    }

    private fun documentedOperations(): Set<String> = paths().entries.flatMap { (path, item) ->
        (item as Map<*, *>).keys
            .map { it.toString() }
            .filter { it in HTTP_METHODS }
            .map { "${it.uppercase()} $path" }
    }.toSet()

    private companion object {

        val HTTP_METHODS = listOf("get", "put", "post", "delete", "patch", "options", "head", "trace")

        /** Groups of the Swagger UI menu, one per controller (Conversations is shared). */
        val EXPECTED_TAGS = listOf(
            "Admin Benefit rules",
            "Admin Cases",
            "Admin Catalogs",
            "Admin Identity",
            "Admin Publications",
            "Auth",
            "Cases",
            "Community",
            "Conversations",
            "Favorites",
            "Host Accommodation",
            "Host Community",
            "Host Reservations",
            "Me Identity",
            "Messaging Safety",
            "Notifications",
            "Profile",
            "Publications",
            "Reputation",
            "Reservations",
            "Reviews",
            "Search",
        )

        /** Flows allowed by SecurityConfig without a session (RF-001, RF-003). */
        val PUBLIC_AUTH_OPERATIONS = listOf(
            "POST /api/v1/auth/register",
            "POST /api/v1/auth/verify-email",
            "POST /api/v1/auth/login",
            "POST /api/v1/auth/password-reset/request",
            "POST /api/v1/auth/password-reset",
        )

        /** Every operation of the API, one per controller mapping. */
        val EXPECTED_OPERATIONS: Set<String> = setOf(
            "DELETE /api/v1/admin/catalogs/{category}/{id}",
            "DELETE /api/v1/favorites/{publicationId}",
            "DELETE /api/v1/host/units/{id}/availability/blocks",
            "DELETE /api/v1/users/me/blocks/{blockedUserId}",
            "GET /api/v1/admin/benefit-rules",
            "GET /api/v1/admin/cases",
            "GET /api/v1/admin/cases/metrics",
            "GET /api/v1/admin/cases/{id}",
            "GET /api/v1/admin/catalogs/{category}",
            "GET /api/v1/admin/publications",
            "GET /api/v1/admin/roles",
            "GET /api/v1/admin/verification-documents",
            "GET /api/v1/cases",
            "GET /api/v1/community/activities",
            "GET /api/v1/community/activities/{id}",
            "GET /api/v1/community/summary",
            "GET /api/v1/conversations",
            "GET /api/v1/conversations/reports",
            "GET /api/v1/conversations/{id}",
            "GET /api/v1/favorites",
            "GET /api/v1/host/community/activities",
            "GET /api/v1/host/community/activities/{id}",
            "GET /api/v1/host/properties",
            "GET /api/v1/host/properties/{id}/units",
            "GET /api/v1/host/reservations",
            "GET /api/v1/host/reservations/{id}/history",
            "GET /api/v1/notifications",
            "GET /api/v1/profile",
            "GET /api/v1/publications",
            "GET /api/v1/publications/{id}",
            "GET /api/v1/publications/{id}/availability",
            "GET /api/v1/reservations/mine",
            "GET /api/v1/reviews/mine",
            "GET /api/v1/reviews/received/{userId}",
            "GET /api/v1/search/listings",
            "GET /api/v1/users/me",
            "GET /api/v1/users/me/blocks",
            "GET /api/v1/users/me/verification-documents",
            "GET /api/v1/users/{userId}/reputation",
            "PATCH /api/v1/host/properties/{id}",
            "PATCH /api/v1/host/units/{id}",
            "PATCH /api/v1/profile",
            "POST /api/v1/admin/benefit-rules",
            "POST /api/v1/admin/benefit-rules/{id}/deactivate",
            "POST /api/v1/admin/cases/{id}/assign",
            "POST /api/v1/admin/cases/{id}/reject",
            "POST /api/v1/admin/cases/{id}/resolve",
            "POST /api/v1/admin/cases/{id}/review",
            "POST /api/v1/admin/catalogs/{category}",
            "POST /api/v1/admin/publications/{id}/review",
            "POST /api/v1/admin/users/{userId}/roles",
            "POST /api/v1/admin/verification-documents/{id}/approve",
            "POST /api/v1/admin/verification-documents/{id}/reject",
            "POST /api/v1/admin/verification-documents/{id}/request-correction",
            "POST /api/v1/auth/login",
            "POST /api/v1/auth/logout",
            "POST /api/v1/auth/password-reset",
            "POST /api/v1/auth/password-reset/request",
            "POST /api/v1/auth/register",
            "POST /api/v1/auth/verify-email",
            "POST /api/v1/community/activities/{id}/confirm",
            "POST /api/v1/community/activities/{id}/withdraw",
            "POST /api/v1/conversations",
            "POST /api/v1/conversations/{id}/messages",
            "POST /api/v1/conversations/{id}/reports",
            "POST /api/v1/favorites/{publicationId}",
            "POST /api/v1/host/community/activities",
            "POST /api/v1/host/community/activities/{id}/cancel",
            "POST /api/v1/host/properties",
            "POST /api/v1/host/properties/{id}/units",
            "POST /api/v1/host/publications/{id}/archive",
            "POST /api/v1/host/publications/{id}/hide",
            "POST /api/v1/host/publications/{id}/pause",
            "POST /api/v1/host/publications/{id}/publish",
            "POST /api/v1/host/publications/{id}/request-review",
            "POST /api/v1/host/reservations/{id}/accept",
            "POST /api/v1/host/reservations/{id}/cancel",
            "POST /api/v1/host/reservations/{id}/reject",
            "POST /api/v1/host/reservations/{id}/request-info",
            "POST /api/v1/host/units/{id}/publication",
            "POST /api/v1/notifications/{id}/read",
            "POST /api/v1/publications/{id}/reports",
            "POST /api/v1/reservations",
            "POST /api/v1/reservations/{id}/cancel",
            "POST /api/v1/reservations/{id}/confirm",
            "POST /api/v1/reservations/{reservationId}/messages",
            "POST /api/v1/reviews",
            "POST /api/v1/reviews/{id}/dispute",
            "POST /api/v1/users/me/blocks",
            "POST /api/v1/users/me/verification-documents",
            "PUT /api/v1/admin/catalogs/{category}/{id}",
            "PUT /api/v1/host/publications/{id}/catalog-references",
            "PUT /api/v1/host/publications/{id}/pricing",
            "PUT /api/v1/host/publications/{id}/rules",
            "PUT /api/v1/host/units/{id}/availability/blocks",
            "PUT /api/v1/profile/privacy",
            "PUT /api/v1/users/me/consents/{type}",
            "PUT /api/v1/users/me/role",
        )
    }
}
