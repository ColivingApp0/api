package com.coliving.api

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Shared base for full-context integration tests. Starts one ephemeral
 * PostgreSQL container per JVM (shares the companion object across subclass
 * classes) and points Spring's datasource at it, so `./gradlew test` never
 * needs a local database.
 */
abstract class AbstractPostgresIntegrationTest {

    companion object {
        private val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("coliving")
                .withUsername("postgres")
                .withPassword("postgres")
                .apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
        }
    }
}