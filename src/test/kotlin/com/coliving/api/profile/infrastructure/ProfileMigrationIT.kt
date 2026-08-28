package com.coliving.api.profile.infrastructure

import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.assertEquals

/**
 * End-to-end migration test (Testcontainers): applies 1.0.0, seeds populated
 * `identity_profile` rows, then applies 1.0.1 and verifies the data landed in
 * `profile_profile` while the legacy table is dropped.
 *
 * It uses its own dedicated database (never shared with the production-master
 * context) so the seeded migration path always runs and is deterministic.
 */
@SpringBootTest
class ProfileMigrationIT {

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    companion object {
        private val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("coliving_migration")
                .withUsername("postgres")
                .withPassword("postgres")
                .apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun migrationProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.liquibase.change-log") { "classpath:/db/test-changelog-master.yaml" }
        }
    }

    private val migratedUserId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val cityId = UUID.fromString("22222222-2222-2222-2222-222222222222")

    @Test
    fun `migrates populated identity_profile rows into profile_profile`() {
        val rows = jdbcTemplate.queryForList(
            """
            SELECT user_id, full_name, phone, guest_type, home_city_id,
                   stay_preferences, affinity_visible, academic_unlisted,
                   institution_id, faculty_id, career_id, academic_visible
            FROM profile_profile
            WHERE user_id = ?
            """.trimIndent(),
            migratedUserId,
        )

        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("Migrated User", row["full_name"])
        assertEquals("3001234567", row["phone"])
        assertEquals("ESTUDIANTE", row["guest_type"])
        assertEquals(cityId.toString(), row["home_city_id"].toString())
        assertEquals("wifi,pet", row["stay_preferences"])
        assertEquals(true, row["affinity_visible"])
        // No 1.0.0 source for the academic reference: it starts empty.
        assertEquals(false, row["academic_unlisted"])
        assertEquals(false, row["academic_visible"])
        assertEquals(null, row["institution_id"])
        assertEquals(null, row["faculty_id"])
        assertEquals(null, row["career_id"])
    }

    @Test
    fun `drops identity_profile after the copy`() {
        val legacyTables = jdbcTemplate.queryForList(
            """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public' AND table_name = 'identity_profile'
            """.trimIndent(),
        )
        assertEquals(0, legacyTables.size)
    }
}