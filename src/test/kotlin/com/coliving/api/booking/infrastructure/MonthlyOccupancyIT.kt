package com.coliving.api.booking.infrastructure

import com.coliving.api.AbstractPostgresIntegrationTest
import com.coliving.api.accommodation.application.dto.CreatePropertyCommand
import com.coliving.api.accommodation.application.dto.CreateUnitCommand
import com.coliving.api.accommodation.application.dto.SetUnitAvailabilityCommand
import com.coliving.api.accommodation.application.usecase.PropertyService
import com.coliving.api.accommodation.application.usecase.PublicationService
import com.coliving.api.accommodation.application.usecase.UnitService
import com.coliving.api.accommodation.domain.enums.BathroomType
import com.coliving.api.accommodation.domain.enums.RoomType
import com.coliving.api.booking.application.dto.CreateReservationCommand
import com.coliving.api.booking.application.dto.HostDecisionCommand
import com.coliving.api.booking.application.query.ReservationOccupancyQuery
import com.coliving.api.booking.application.usecase.AcceptReservationService
import com.coliving.api.booking.application.usecase.CreateReservationService
import com.coliving.api.shared.error.ConflictException
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Monthly occupancy rules against real PostgreSQL: acceptance occupies a place,
 * cancellation frees it, pausing stops requests, and two concurrent acceptances
 * cannot take the same last place (row lock on the room).
 */
@SpringBootTest
class MonthlyOccupancyIT : AbstractPostgresIntegrationTest() {

    @Autowired lateinit var propertyService: PropertyService
    @Autowired lateinit var unitService: UnitService
    @Autowired lateinit var publicationService: PublicationService
    @Autowired lateinit var createReservationService: CreateReservationService
    @Autowired lateinit var acceptReservationService: AcceptReservationService
    @Autowired lateinit var occupancyQuery: ReservationOccupancyQuery
    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `acceptance occupies the last place and a full room rejects new requests`() {
        val host = insertUser("host-occupancy")
        val guestA = insertUser("guest-a")
        val guestB = insertUser("guest-b")
        val guestC = insertUser("guest-c")
        listOf(host, guestA, guestB, guestC).forEach { approve(it) }

        val room = publishedRoom(host, capacity = 2)
        val requestA = createReservationService.create(CreateReservationCommand(guestA, room, 1, null))
        val requestB = createReservationService.create(CreateReservationCommand(guestB, room, 1, null))

        assertEquals(0, occupancyQuery.occupiedPlacesOf(listOf(room))[room] ?: 0)
        // Pending requests are not occupancy.
        assertEquals(0, occupancyQuery.occupiedPlacesOf(listOf(room))[room] ?: 0)

        acceptReservationService.accept(HostDecisionCommand(requestA.id, host, null))
        assertEquals(1, occupancyQuery.occupiedPlacesOf(listOf(room))[room])

        acceptReservationService.accept(HostDecisionCommand(requestB.id, host, null))
        assertEquals(2, occupancyQuery.occupiedPlacesOf(listOf(room))[room])

        assertFailsWith<ConflictException> {
            createReservationService.create(CreateReservationCommand(guestC, room, 1, null))
        }
    }

    @Test
    fun `a paused room rejects requests even with free places`() {
        val host = insertUser("host-paused")
        val guest = insertUser("guest-paused")
        listOf(host, guest).forEach { approve(it) }

        val room = publishedRoom(host, capacity = 1)
        unitService.setAvailability(SetUnitAvailabilityCommand(room, host, false))

        assertFailsWith<ConflictException> {
            createReservationService.create(CreateReservationCommand(guest, room, 1, null))
        }
    }

    @Test
    fun `two concurrent acceptances cannot take the same last place`() {
        val host = insertUser("host-race")
        val guestA = insertUser("guest-race-a")
        val guestB = insertUser("guest-race-b")
        listOf(host, guestA, guestB).forEach { approve(it) }

        val room = publishedRoom(host, capacity = 1)
        val requestA = createReservationService.create(CreateReservationCommand(guestA, room, 1, null))
        val requestB = createReservationService.create(CreateReservationCommand(guestB, room, 1, null))

        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        val tasks = listOf(requestA.id, requestB.id).map { id ->
            Callable {
                ready.countDown()
                start.await(5, TimeUnit.SECONDS)
                runCatching { acceptReservationService.accept(HostDecisionCommand(id, host, null)) }
            }
        }
        val futures = tasks.map { pool.submit(it) }
        ready.await(5, TimeUnit.SECONDS)
        start.countDown()
        val results = futures.map { it.get(20, TimeUnit.SECONDS) }
        pool.shutdownNow()

        assertEquals(1, results.count { it.isSuccess }, "Exactly one acceptance must win the last place")
        assertTrue(results.filter { it.isFailure }.all { it.exceptionOrNull() is ConflictException })
        assertEquals(1, occupancyQuery.occupiedPlacesOf(listOf(room))[room])
    }

    @Test
    fun `roommate identity requires explicit consent`() {
        val host = insertUser("host-privacy")
        val resident = insertUser("resident-privacy")
        listOf(host, resident).forEach { approve(it) }

        val room = publishedRoom(host, capacity = 2)
        val request = createReservationService.create(CreateReservationCommand(resident, room, 1, null))
        acceptReservationService.accept(HostDecisionCommand(request.id, host, null))
        val publicationId = publicationIdOf(room)

        jdbcTemplate.update(
            """
            INSERT INTO profile_profile (user_id, full_name, roommate_profile_visible, affinity_visible,
                academic_unlisted, academic_visible, updated_at)
            VALUES (?, 'Residente Reservado', FALSE, FALSE, FALSE, FALSE, CURRENT_TIMESTAMP)
            ON CONFLICT (user_id) DO UPDATE SET full_name = EXCLUDED.full_name,
                roommate_profile_visible = EXCLUDED.roommate_profile_visible
            """.trimIndent(),
            resident,
        )
        assertTrue(publicationService.roommates(publicationId).isEmpty())

        jdbcTemplate.update(
            "UPDATE profile_profile SET roommate_profile_visible = TRUE WHERE user_id = ?",
            resident,
        )
        val roommates = publicationService.roommates(publicationId)
        assertEquals(1, roommates.size)
        assertEquals("Residente Reservado", roommates.single().name)
    }

    private fun publishedRoom(host: UUID, capacity: Int): UUID {
        val property = propertyService.create(
            CreatePropertyCommand(hostId = host, title = "Casa de prueba", cityName = "Bogotá"),
        )
        val unit = unitService.create(
            CreateUnitCommand(
                propertyId = property.id,
                hostId = host,
                name = "Habitación ${UUID.randomUUID().toString().take(6)}",
                maxGuests = capacity,
                monthlyPrice = BigDecimal("750000"),
                roomType = if (capacity > 1) RoomType.COMPARTIDA else RoomType.INDIVIDUAL,
                bathroomType = BathroomType.COMPARTIDO,
                airConditioning = true,
            ),
        )
        publicationService.publish(publicationIdOf(unit.id), host)
        return unit.id
    }

    private fun publicationIdOf(unitId: UUID): UUID =
        jdbcTemplate.queryForObject(
            "SELECT id FROM accommodation_publication WHERE unit_id = ?",
            UUID::class.java,
            unitId,
        )!!

    private fun insertUser(prefix: String): UUID {
        val id = UUID.randomUUID()
        jdbcTemplate.update(
            """
            INSERT INTO identity_user
                (id, email, password_hash, status, email_verified, created_at, updated_at)
            VALUES (?, ?, 'test-hash', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """.trimIndent(),
            id,
            "$prefix-$id@example.com",
        )
        return id
    }

    private fun approve(userId: UUID) {
        jdbcTemplate.update(
            """
            INSERT INTO identity_verification_document
                (id, user_id, document_type, storage_key, status, uploaded_at)
            VALUES (?, ?, 'IDENTIDAD', 'test-key', 'APROBADO', CURRENT_TIMESTAMP)
            """.trimIndent(),
            UUID.randomUUID(),
            userId,
        )
    }
}
