package com.coliving.api.accommodation.application

import com.coliving.api.accommodation.application.port.out.HostVerificationPort
import com.coliving.api.accommodation.application.port.out.PublicProfileCard
import com.coliving.api.accommodation.application.port.out.PublicProfilePort
import com.coliving.api.accommodation.application.port.out.UnitOccupancyPort
import com.coliving.api.accommodation.application.usecase.AccommodationViewAssembler
import com.coliving.api.accommodation.application.usecase.PropertyService
import com.coliving.api.accommodation.application.usecase.PublicationService
import com.coliving.api.accommodation.domain.enums.BathroomType
import com.coliving.api.accommodation.domain.enums.RoomType
import com.coliving.api.accommodation.domain.model.Property
import com.coliving.api.accommodation.domain.model.PropertyPhoto
import com.coliving.api.accommodation.domain.model.Publication
import com.coliving.api.accommodation.domain.model.Unit
import com.coliving.api.accommodation.domain.model.UnitPhoto
import com.coliving.api.accommodation.domain.repository.PropertyPhotoRepository
import com.coliving.api.accommodation.domain.repository.PropertyRepository
import com.coliving.api.accommodation.domain.repository.PublicationRepository
import com.coliving.api.accommodation.domain.repository.UnitPhotoRepository
import com.coliving.api.accommodation.domain.repository.UnitRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The exact street address is private: guest-facing projections expose only
 * city/neighborhood, while host endpoints keep the address for editing.
 */
class PublicPropertyAddressPrivacyTest {

    @Test
    fun `public property detail hides the exact address`() {
        val scenario = scenario()

        val detail = assertNotNull(scenario.propertyService.publicDetail(scenario.property.id))

        assertNull(detail.property.address)
        assertEquals(CITY_NAME, detail.property.cityName)
        assertEquals(NEIGHBORHOOD, detail.property.neighborhood)
    }

    @Test
    fun `public publication detail hides the exact address`() {
        val scenario = scenario()

        val detail = assertNotNull(scenario.publicationService.findPublishedDetail(scenario.publication.id))

        assertNull(detail.property.address)
        assertEquals(CITY_NAME, detail.property.cityName)
        assertEquals(NEIGHBORHOOD, detail.property.neighborhood)
    }

    @Test
    fun `host property detail keeps the exact address`() {
        val scenario = scenario()

        val detail = scenario.propertyService.hostDetail(scenario.property.id, HOST_ID)

        assertEquals(ADDRESS, detail.property.address)
    }

    @Test
    fun `host property list keeps the exact address`() {
        val scenario = scenario()

        assertEquals(ADDRESS, scenario.propertyService.listByHost(HOST_ID).single().address)
    }

    private fun scenario(): Scenario = Scenario()

    private class Scenario {
        val propertyRepository = InMemoryPropertyRepository()
        val unitRepository = InMemoryUnitRepository()
        val publicationRepository = InMemoryPublicationRepository()

        val property: Property
        val publication: Publication
        val propertyService: PropertyService
        val publicationService: PublicationService

        init {
            property = Property.create(
                id = UUID.randomUUID(),
                hostId = HOST_ID,
                title = "Casa compartida",
                description = "Casa con habitaciones amobladas",
                cityId = UUID.randomUUID(),
                cityName = CITY_NAME,
                neighborhood = NEIGHBORHOOD,
                address = ADDRESS,
                amenities = listOf("WiFi"),
            )
            propertyRepository.save(property)

            val unit = Unit.create(
                id = UUID.randomUUID(),
                propertyId = property.id,
                name = "Habitación privada",
                maxGuests = 1,
                monthlyPrice = BigDecimal("900000"),
                roomType = RoomType.INDIVIDUAL,
                bathroomType = BathroomType.PRIVADO,
            )
            unitRepository.save(unit)

            publication = Publication.create(
                id = UUID.randomUUID(),
                unitId = unit.id,
                title = unit.name,
                now = Instant.now(),
            ).also { it.publish() }
            publicationRepository.save(publication)

            val assembler = AccommodationViewAssembler(
                publicationRepository = publicationRepository,
                propertyPhotoRepository = EmptyPropertyPhotoRepository,
                unitPhotoRepository = EmptyUnitPhotoRepository,
                occupancyPort = EmptyOccupancyPort,
                publicProfilePort = EmptyPublicProfilePort,
            )
            propertyService = PropertyService(
                propertyRepository = propertyRepository,
                unitRepository = unitRepository,
                publicationRepository = publicationRepository,
                propertyPhotoRepository = EmptyPropertyPhotoRepository,
                occupancyPort = EmptyOccupancyPort,
                assembler = assembler,
            )
            publicationService = PublicationService(
                publicationRepository = publicationRepository,
                unitRepository = unitRepository,
                propertyRepository = propertyRepository,
                hostVerificationPort = AlwaysVerifiedHostPort,
                assembler = assembler,
            )
        }
    }

    private companion object {
        val HOST_ID: UUID = UUID.randomUUID()
        const val CITY_NAME = "Bogotá"
        const val NEIGHBORHOOD = "Chapinero"
        const val ADDRESS = "Calle 63 # 12-34, Bogotá"
    }
}

private class InMemoryPropertyRepository : PropertyRepository {
    private val store = mutableMapOf<UUID, Property>()

    override fun findById(id: UUID): Property? = store[id]

    override fun findByHost(hostId: UUID): List<Property> = store.values.filter { it.hostId == hostId }

    override fun save(property: Property) {
        store[property.id] = property
    }
}

private class InMemoryUnitRepository : UnitRepository {
    private val store = mutableMapOf<UUID, Unit>()

    override fun findById(id: UUID): Unit? = store[id]

    override fun findByIdForUpdate(id: UUID): Unit? = store[id]

    override fun findByProperty(propertyId: UUID): List<Unit> = store.values.filter { it.propertyId == propertyId }

    override fun save(unit: Unit) {
        store[unit.id] = unit
    }
}

private class InMemoryPublicationRepository : PublicationRepository {
    private val store = mutableMapOf<UUID, Publication>()

    override fun findById(id: UUID): Publication? = store[id]

    override fun findByUnit(unitId: UUID): Publication? = store.values.firstOrNull { it.unitId == unitId }

    override fun findByStatus(status: com.coliving.api.accommodation.domain.enums.PublicationStatus): List<Publication> =
        store.values.filter { it.status == status }

    override fun findAllPublished(): List<Publication> =
        store.values.filter { it.status == com.coliving.api.accommodation.domain.enums.PublicationStatus.PUBLICADA }

    override fun save(publication: Publication) {
        store[publication.id] = publication
    }
}

private object EmptyPropertyPhotoRepository : PropertyPhotoRepository {
    override fun findByProperty(propertyId: UUID): List<PropertyPhoto> = emptyList()

    override fun findById(id: UUID): PropertyPhoto? = null

    override fun save(photo: PropertyPhoto) {}

    override fun delete(photo: PropertyPhoto) {}
}

private object EmptyUnitPhotoRepository : UnitPhotoRepository {
    override fun findByUnit(unitId: UUID): List<UnitPhoto> = emptyList()

    override fun findById(id: UUID): UnitPhoto? = null

    override fun save(photo: UnitPhoto) {}

    override fun delete(photo: UnitPhoto) {}
}

private object EmptyOccupancyPort : UnitOccupancyPort {
    override fun occupiedPlacesOf(unitIds: Collection<UUID>): Map<UUID, Int> = emptyMap()

    override fun residentIdsOf(unitId: UUID): List<UUID> = emptyList()
}

private object EmptyPublicProfilePort : PublicProfilePort {
    override fun findPublicProfile(userId: UUID): PublicProfileCard? = null
}

private object AlwaysVerifiedHostPort : HostVerificationPort {
    override fun isVerifiedHost(userId: UUID): Boolean = true
}
