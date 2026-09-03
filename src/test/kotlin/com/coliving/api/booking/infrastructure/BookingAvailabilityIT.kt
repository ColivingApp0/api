package com.coliving.api.booking.infrastructure

import com.coliving.api.AbstractPostgresIntegrationTest
import com.coliving.api.accommodation.application.dto.ConfigurePricingCommand
import com.coliving.api.accommodation.application.dto.CreatePropertyCommand
import com.coliving.api.accommodation.application.dto.CreatePublicationCommand
import com.coliving.api.accommodation.application.dto.CreateUnitCommand
import com.coliving.api.accommodation.application.usecase.AvailabilityLockService
import com.coliving.api.accommodation.application.usecase.PropertyService
import com.coliving.api.accommodation.application.usecase.PublicationService
import com.coliving.api.accommodation.application.usecase.UnitService
import com.coliving.api.accommodation.domain.enums.AvailabilityState
import com.coliving.api.booking.application.dto.CancelReservationCommand
import com.coliving.api.booking.application.dto.ConfirmReservationCommand
import com.coliving.api.booking.application.dto.CreateReservationCommand
import com.coliving.api.booking.application.dto.HostDecisionCommand
import com.coliving.api.booking.application.usecase.AcceptReservationService
import com.coliving.api.booking.application.usecase.CancelReservationService
import com.coliving.api.booking.application.usecase.ConfirmReservationService
import com.coliving.api.booking.application.usecase.CreateReservationService
import com.coliving.api.booking.domain.enums.ReservationStatus
import com.coliving.api.shared.error.ConflictException
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Full booking flow against real PostgreSQL: create -> accept -> confirm ->
 * cancel, verifying the accommodation slot states and the shared
 * no-double-booking invariant end to end (RF-040..RF-044, RN-03).
 */
@SpringBootTest
class BookingAvailabilityIT : AbstractPostgresIntegrationTest() {

    @Autowired lateinit var propertyService: PropertyService
    @Autowired lateinit var unitService: UnitService
    @Autowired lateinit var publicationService: PublicationService
    @Autowired lateinit var availabilityLockService: AvailabilityLockService
    @Autowired lateinit var createReservationService: CreateReservationService
    @Autowired lateinit var acceptReservationService: AcceptReservationService
    @Autowired lateinit var confirmReservationService: ConfirmReservationService
    @Autowired lateinit var cancelReservationService: CancelReservationService
    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    private val from = LocalDate.now().plusDays(10)
    private val to = LocalDate.now().plusDays(14)

    @Test
    fun `full reservation lifecycle transitions the unit availability`() {
        val hostId = insertUser("host")
        val guestId = insertUser("guest")
        insertApprovedDocument(hostId)
        insertApprovedDocument(guestId)
        val unitId = seedBookableUnit(hostId)

        val created = createReservationService.create(
            CreateReservationCommand(
                guestId = guestId,
                unitId = unitId,
                fromDate = from,
                toDate = to,
                occupants = 2,
                message = "Me interesa",
            ),
        )
        assertEquals(ReservationStatus.SOLICITADA, created.status)

        val accepted = acceptReservationService.accept(
            HostDecisionCommand(reservationId = created.id, hostId = hostId, reason = null),
        )
        assertEquals(ReservationStatus.ACEPTADA, accepted.status)
        assertTrue { accepted.holdExpiresAt != null }
        assertNights(unitId, AvailabilityState.BLOQUEADO)

        val confirmed = confirmReservationService.confirm(
            ConfirmReservationCommand(reservationId = created.id, guestId = guestId),
        )
        assertEquals(ReservationStatus.CONFIRMADA, confirmed.status)
        assertNights(unitId, AvailabilityState.OCUPADO)

        cancelReservationService.cancel(
            CancelReservationCommand(reservationId = created.id, actorId = hostId, reason = "Mantenimiento"),
        )
        assertNights(unitId, AvailabilityState.DISPONIBLE)
    }

    @Test
    fun `a second request over held nights is rejected (no double booking)`() {
        val hostId = insertUser("host2")
        val guest1 = insertUser("guest2a")
        val guest2 = insertUser("guest2b")
        insertApprovedDocument(hostId)
        insertApprovedDocument(guest1)
        insertApprovedDocument(guest2)
        val unitId = seedBookableUnit(hostId)

        val first = createReservationService.create(
            CreateReservationCommand(guest1, unitId, from, to, 1, null),
        )
        acceptReservationService.accept(HostDecisionCommand(first.id, hostId, null))

        val second = assertFailsWith<ConflictException> {
            createReservationService.create(
                CreateReservationCommand(guest2, unitId, from.plusDays(1), to.plusDays(1), 1, null),
            )
        }
        assertTrue { second.message!!.contains("not available") }
    }

    // ---------- helpers ----------

    private fun seedBookableUnit(hostId: UUID): UUID {
        val property = propertyService.create(CreatePropertyCommand(hostId = hostId, title = "Casa booking"))
        val unit = unitService.create(
            CreateUnitCommand(
                propertyId = property.id,
                hostId = hostId,
                name = "Habitación",
                maxGuests = 3,
                bedrooms = 1,
                beds = 1,
                bathrooms = 1,
            ),
        )
        val publication = publicationService.createPublication(
            CreatePublicationCommand(unitId = unit.id, hostId = hostId, title = "Habitación amplia"),
        )
        publicationService.configurePricing(
            ConfigurePricingCommand(
                publicationId = publication.id,
                hostId = hostId,
                basePricePerNight = BigDecimal("90000"),
                currency = com.coliving.api.accommodation.domain.enums.Currency.COP,
            ),
        )
        publicationService.publish(publication.id, hostId)
        return unit.id
    }

    private fun assertNights(unitId: UUID, expected: AvailabilityState) {
        val nights = availabilityLockService.getAvailability(unitId, from, to)
        assertEquals(4, nights.size)
        nights.forEach { assertEquals(expected, it.state) }
    }

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

    private fun insertApprovedDocument(userId: UUID) {
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
