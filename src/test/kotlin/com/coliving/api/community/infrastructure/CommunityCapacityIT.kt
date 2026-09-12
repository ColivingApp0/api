package com.coliving.api.community.infrastructure

import com.coliving.api.AbstractPostgresIntegrationTest
import com.coliving.api.accommodation.application.dto.ConfigurePricingCommand
import com.coliving.api.accommodation.application.dto.CreatePropertyCommand
import com.coliving.api.accommodation.application.dto.CreatePublicationCommand
import com.coliving.api.accommodation.application.dto.CreateUnitCommand
import com.coliving.api.accommodation.application.usecase.PropertyService
import com.coliving.api.accommodation.application.usecase.PublicationService
import com.coliving.api.accommodation.application.usecase.UnitService
import com.coliving.api.booking.application.dto.ConfirmReservationCommand
import com.coliving.api.booking.application.dto.CreateReservationCommand
import com.coliving.api.booking.application.dto.HostDecisionCommand
import com.coliving.api.booking.application.usecase.AcceptReservationService
import com.coliving.api.booking.application.usecase.ConfirmReservationService
import com.coliving.api.booking.application.usecase.CreateReservationService
import com.coliving.api.community.application.dto.ConfirmAttendanceCommand
import com.coliving.api.community.application.dto.CreateActivityCommand
import com.coliving.api.community.application.dto.WithdrawFromActivityCommand
import com.coliving.api.community.application.usecase.CommunitySummaryService
import com.coliving.api.community.application.usecase.ConfirmAttendanceService
import com.coliving.api.community.application.usecase.CreateActivityService
import com.coliving.api.community.application.usecase.WithdrawFromActivityService
import com.coliving.api.community.domain.enums.ParticipationStatus
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import java.math.BigDecimal
import java.time.Instant
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
 * Community flow against real PostgreSQL (Testcontainers), exercising the real
 * cross-context providers: residency comes from booking (confirmed stays) over
 * the units read from accommodation, and the consent gate from identity.
 *
 * It verifies the capacity invariant of RF-061 — never more confirmations than
 * places — and that only the authorized community can take part (RF-060).
 */
@SpringBootTest
class CommunityCapacityIT : AbstractPostgresIntegrationTest() {

    @Autowired lateinit var propertyService: PropertyService
    @Autowired lateinit var unitService: UnitService
    @Autowired lateinit var publicationService: PublicationService
    @Autowired lateinit var createReservationService: CreateReservationService
    @Autowired lateinit var acceptReservationService: AcceptReservationService
    @Autowired lateinit var confirmReservationService: ConfirmReservationService
    @Autowired lateinit var createActivityService: CreateActivityService
    @Autowired lateinit var confirmAttendanceService: ConfirmAttendanceService
    @Autowired lateinit var withdrawFromActivityService: WithdrawFromActivityService
    @Autowired lateinit var communitySummaryService: CommunitySummaryService
    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    private val from = LocalDate.now().plusDays(20)
    private val to = LocalDate.now().plusDays(24)

    @Test
    fun `capacity and authorized community are enforced end to end (RF-060, RF-061)`() {
        val hostId = insertUser("host-community")
        val residentA = insertUser("resident-a")
        val residentB = insertUser("resident-b")
        val outsider = insertUser("outsider")
        listOf(hostId, residentA, residentB, outsider).forEach { insertApprovedDocument(it) }

        val propertyId = propertyService.create(CreatePropertyCommand(hostId = hostId, title = "Casa comunidad")).id
        confirmStay(hostId, residentA, propertyId, "Habitación A")
        confirmStay(hostId, residentB, propertyId, "Habitación B")

        // The host creates the activity for the property's community.
        val detail = createActivityService.create(
            CreateActivityCommand(
                hostId = hostId,
                propertyId = propertyId,
                title = "Asado de bienvenida",
                description = "Trae algo para compartir",
                scheduledAt = Instant.now().plusSeconds(86_400 * 5),
                capacity = 1,
                enabledParticipantIds = listOf(residentA, residentB),
            ),
        )
        val activityId = detail.activity.id
        assertEquals(2, detail.participants.size)
        assertTrue { detail.participants.all { it.status == ParticipationStatus.HABILITADO } }
        assertEquals(1, detail.activity.freePlaces)

        // The only place goes to the first resident.
        val confirmed = confirmAttendanceService.confirm(ConfirmAttendanceCommand(activityId, residentA))
        assertEquals(ParticipationStatus.CONFIRMADA, confirmed.status)
        assertEquals(1, communitySummaryService.summaryOf(propertyId).upcomingActivityCount)

        // The capacity is a hard limit.
        assertFailsWith<ConflictException> {
            confirmAttendanceService.confirm(ConfirmAttendanceCommand(activityId, residentB))
        }

        // Withdrawing frees the place: the list stays consistent (RF-061).
        withdrawFromActivityService.withdraw(WithdrawFromActivityCommand(activityId, residentA))
        val second = confirmAttendanceService.confirm(ConfirmAttendanceCommand(activityId, residentB))
        assertEquals(ParticipationStatus.CONFIRMADA, second.status)

        // Somebody outside the authorized community cannot take part (RF-060).
        assertFailsWith<ForbiddenException> {
            confirmAttendanceService.confirm(ConfirmAttendanceCommand(activityId, outsider))
        }
    }

    /** Publishes a unit in [propertyId] and drives a reservation to CONFIRMADA. */
    private fun confirmStay(hostId: UUID, guestId: UUID, propertyId: UUID, unitName: String): UUID {
        val unit = unitService.create(
            CreateUnitCommand(
                propertyId = propertyId,
                hostId = hostId,
                name = unitName,
                maxGuests = 2,
                bedrooms = 1,
                beds = 1,
                bathrooms = 1,
            ),
        )
        val publication = publicationService.createPublication(
            CreatePublicationCommand(unitId = unit.id, hostId = hostId, title = unitName),
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

        val reservation = createReservationService.create(
            CreateReservationCommand(
                guestId = guestId,
                unitId = unit.id,
                fromDate = from,
                toDate = to,
                occupants = 1,
                message = null,
            ),
        )
        acceptReservationService.accept(HostDecisionCommand(reservation.id, hostId, null))
        confirmReservationService.confirm(ConfirmReservationCommand(reservation.id, guestId))
        return unit.id
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