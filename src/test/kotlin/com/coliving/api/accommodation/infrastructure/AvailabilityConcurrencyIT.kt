package com.coliving.api.accommodation.infrastructure

import com.coliving.api.AbstractPostgresIntegrationTest
import com.coliving.api.accommodation.application.dto.CreatePropertyCommand
import com.coliving.api.accommodation.application.dto.CreateUnitCommand
import com.coliving.api.accommodation.application.usecase.AvailabilityLockService
import com.coliving.api.accommodation.application.usecase.PropertyService
import com.coliving.api.accommodation.application.usecase.UnitService
import com.coliving.api.accommodation.domain.enums.AvailabilityState
import com.coliving.api.shared.error.ConflictException
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The core no-double-booking guarantee against a real PostgreSQL instance:
 * two transactions racing to lock the same nights serialize on
 * `SELECT ... FOR UPDATE`; exactly one wins, the other sees BLOQUEADO and
 * rolls back with ConflictException.
 */
@SpringBootTest
class AvailabilityConcurrencyIT : AbstractPostgresIntegrationTest() {

    @Autowired
    lateinit var propertyService: PropertyService

    @Autowired
    lateinit var unitService: UnitService

    @Autowired
    lateinit var availabilityLockService: AvailabilityLockService

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private val from = LocalDate.of(2026, 10, 1)
    private val to = LocalDate.of(2026, 10, 5)

    @Test
    fun `concurrent locks on the same nights cannot both succeed`() {
        val hostId = insertHost()

        val property = propertyService.create(
            CreatePropertyCommand(hostId = hostId, title = "Casa prueba"),
        )
        val unit = unitService.create(
            CreateUnitCommand(
                propertyId = property.id,
                hostId = hostId,
                name = "Habitación",
                maxGuests = 2,
                bedrooms = 1,
                beds = 1,
                bathrooms = 1,
            ),
        )

        val executor = Executors.newFixedThreadPool(2)
        val results = (1..2).map { idx ->
            executor.submit<String> {
                try {
                    availabilityLockService.lockRange(unit.id, from, to, UUID.randomUUID())
                    "OK"
                } catch (e: ConflictException) {
                    "CONFLICT"
                }
            }
        }.map { it.get(30, TimeUnit.SECONDS) }
        executor.shutdown()

        assertEquals(1, results.count { it == "OK" })
        assertEquals(1, results.count { it == "CONFLICT" })

        val nights = availabilityLockService.getAvailability(unit.id, from, to)
        assertEquals(4, nights.size)
        nights.forEach { assertEquals(AvailabilityState.BLOQUEADO, it.state) }
        assertTrue { nights.all { it.reserved } }
    }

    private fun insertHost(): UUID {
        val hostId = UUID.randomUUID()
        jdbcTemplate.update(
            """
            INSERT INTO identity_user
                (id, email, password_hash, status, email_verified, created_at, updated_at)
            VALUES (?, ?, 'test-hash', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """.trimIndent(),
            hostId,
            "host-${hostId}@example.com",
        )
        return hostId
    }
}