package com.coliving.api.accommodation.application.usecase

import com.coliving.api.accommodation.application.dto.OccupancyView
import com.coliving.api.accommodation.application.dto.PhotoView
import com.coliving.api.accommodation.application.dto.PropertyView
import com.coliving.api.accommodation.application.dto.PublicationView
import com.coliving.api.accommodation.application.dto.PublicUserView
import com.coliving.api.accommodation.application.dto.RoommateView
import com.coliving.api.accommodation.application.dto.UnitView
import com.coliving.api.accommodation.application.port.out.PublicProfilePort
import com.coliving.api.accommodation.application.port.out.UnitOccupancyPort
import com.coliving.api.accommodation.domain.enums.PublicationStatus
import com.coliving.api.accommodation.domain.model.Property
import com.coliving.api.accommodation.domain.model.Publication
import com.coliving.api.accommodation.domain.model.Unit
import com.coliving.api.accommodation.domain.repository.PropertyPhotoRepository
import com.coliving.api.accommodation.domain.repository.PublicationRepository
import com.coliving.api.accommodation.domain.repository.UnitPhotoRepository
import java.util.UUID
import org.springframework.stereotype.Service

/**
 * Builds the read models shared by host and public endpoints. Occupancy always
 * comes from the booking-owned occupancy port, never from a stored counter.
 */
@Service
class AccommodationViewAssembler(
    private val publicationRepository: PublicationRepository,
    private val propertyPhotoRepository: PropertyPhotoRepository,
    private val unitPhotoRepository: UnitPhotoRepository,
    private val occupancyPort: UnitOccupancyPort,
    private val publicProfilePort: PublicProfilePort,
) {

    fun propertyView(property: Property): PropertyView =
        PropertyView(
            id = property.id,
            hostId = property.hostId,
            title = property.title,
            description = property.description,
            cityId = property.cityId,
            cityName = property.cityName,
            neighborhood = property.neighborhood,
            address = property.address,
            amenities = property.amenities,
            archived = property.archived,
            photos = propertyPhotoRepository.findByProperty(property.id).map {
                PhotoView(it.id, it.url, it.position)
            },
        )

    fun publicationView(publication: Publication): PublicationView =
        PublicationView(
            id = publication.id,
            unitId = publication.unitId,
            title = publication.title,
            status = publication.status,
            services = publication.services,
            applicableRuleCodes = publication.applicableRuleCodes,
        )

    fun unitView(unit: Unit, occupied: Int? = null): UnitView {
        val publication = publicationRepository.findByUnit(unit.id)
        val occupiedPlaces = occupied
            ?: occupancyPort.occupiedPlacesOf(listOf(unit.id))[unit.id]
            ?: 0
        val published = publication?.status == PublicationStatus.PUBLICADA
        val available = unit.availablePlaces(occupiedPlaces)
        return UnitView(
            id = unit.id,
            propertyId = unit.propertyId,
            name = unit.name,
            description = unit.description,
            maxGuests = unit.maxGuests,
            monthlyPrice = unit.monthlyPrice,
            currency = unit.currency,
            priceUnit = unit.priceUnit,
            roomType = unit.roomType,
            bathroomType = unit.bathroomType,
            airConditioning = unit.airConditioning,
            fan = unit.fan,
            commonAreas = unit.commonAreas,
            typeCode = unit.typeCode,
            accessibilityCodes = unit.accessibilityCodes,
            sortOrder = unit.sortOrder,
            photos = unitPhotoRepository.findByUnit(unit.id).map {
                PhotoView(it.id, it.url, it.position)
            },
            publication = publication?.let(::publicationView),
            occupiedPlaces = occupiedPlaces,
            availablePlaces = available,
            published = published,
            effectiveAvailable = published && available > 0,
            availabilityLabel = availabilityLabel(publication?.status, available, unit.maxGuests),
        )
    }

    fun unitViews(units: List<Unit>, occupancy: Map<UUID, Int>): List<UnitView> =
        units.map { unitView(it, occupancy[it.id] ?: 0) }

    fun occupancy(unit: Unit, published: Boolean): OccupancyView {
        val occupied = occupancyPort.occupiedPlacesOf(listOf(unit.id))[unit.id] ?: 0
        val available = unit.availablePlaces(occupied)
        return OccupancyView(
            capacity = unit.maxGuests,
            occupiedPlaces = occupied,
            availablePlaces = available,
            published = published,
            effectiveAvailable = published && available > 0,
            label = availabilityLabel(if (published) PublicationStatus.PUBLICADA else null, available, unit.maxGuests),
        )
    }

    fun hostView(hostId: UUID): PublicUserView? =
        publicProfilePort.findPublicProfile(hostId)?.let {
            PublicUserView(userId = it.userId, name = it.name, avatarUrl = it.avatarUrl)
        }

    /**
     * Current roommates of a room, only those whose profile sharing is
     * explicitly authorized. Aggregated occupancy is public; identity is not.
     */
    fun roommates(unitId: UUID): List<RoommateView> =
        occupancyPort.residentIdsOf(unitId)
            .mapNotNull { publicProfilePort.findPublicProfile(it) }
            .filter { it.roommateProfileVisible && !it.name.isNullOrBlank() }
            .map { RoommateView(userId = it.userId, name = it.name!!, avatarUrl = it.avatarUrl) }

    private fun availabilityLabel(status: PublicationStatus?, available: Int, capacity: Int): String = when {
        status == null || status == PublicationStatus.BORRADOR -> "Borrador"
        status == PublicationStatus.PAUSADA || status == PublicationStatus.OCULTA -> "Pausada"
        status == PublicationStatus.EN_REVISION -> "En revisión"
        status == PublicationStatus.ARCHIVADA -> "Archivada"
        available <= 0 -> "Ocupada"
        capacity > 1 && available == 1 -> "Últimas plazas"
        else -> "Disponible"
    }
}
