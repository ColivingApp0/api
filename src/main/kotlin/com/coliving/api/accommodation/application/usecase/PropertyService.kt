package com.coliving.api.accommodation.application.usecase

import com.coliving.api.accommodation.application.dto.AddPhotoCommand
import com.coliving.api.accommodation.application.dto.CreatePropertyCommand
import com.coliving.api.accommodation.application.dto.DeletePhotoCommand
import com.coliving.api.accommodation.application.dto.PropertyDetailView
import com.coliving.api.accommodation.application.dto.PropertyView
import com.coliving.api.accommodation.application.dto.UpdatePropertyCommand
import com.coliving.api.accommodation.application.port.out.UnitOccupancyPort
import com.coliving.api.accommodation.domain.enums.PublicationStatus
import com.coliving.api.accommodation.domain.model.Property
import com.coliving.api.accommodation.domain.model.PropertyPhoto
import com.coliving.api.accommodation.domain.repository.PropertyPhotoRepository
import com.coliving.api.accommodation.domain.repository.PropertyRepository
import com.coliving.api.accommodation.domain.repository.PublicationRepository
import com.coliving.api.accommodation.domain.repository.UnitRepository
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.InvalidArgumentException
import com.coliving.api.shared.error.NotFoundException
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PropertyService(
    private val propertyRepository: PropertyRepository,
    private val unitRepository: UnitRepository,
    private val publicationRepository: PublicationRepository,
    private val propertyPhotoRepository: PropertyPhotoRepository,
    private val occupancyPort: UnitOccupancyPort,
    private val assembler: AccommodationViewAssembler,
) {

    @Transactional
    fun create(command: CreatePropertyCommand): PropertyView {
        if (command.title.isBlank()) throw InvalidArgumentException("El nombre es obligatorio")
        val property = Property.create(
            id = UUID.randomUUID(),
            hostId = command.hostId,
            title = command.title,
            description = command.description,
            cityId = command.cityId,
            cityName = command.cityName,
            neighborhood = command.neighborhood,
            address = command.address,
            amenities = command.amenities,
        )
        propertyRepository.save(property)
        return assembler.propertyView(property)
    }

    fun findById(id: UUID): PropertyView? =
        propertyRepository.findById(id)?.let(assembler::propertyView)

    fun listByHost(hostId: UUID): List<PropertyView> =
        propertyRepository.findByHost(hostId).map(assembler::propertyView)

    @Transactional
    fun update(command: UpdatePropertyCommand): PropertyView {
        val property = propertyRepository.findById(command.propertyId)
            ?: throw NotFoundException("Propiedad no encontrada")
        checkOwner(property, command.hostId)
        property.update(
            title = command.title ?: property.title,
            description = if (command.description != null) command.description else property.description,
            cityId = command.cityId ?: property.cityId,
            cityName = command.cityName ?: property.cityName,
            neighborhood = command.neighborhood ?: property.neighborhood,
            address = command.address ?: property.address,
            amenities = command.amenities,
        )
        propertyRepository.save(property)
        return assembler.propertyView(property)
    }

    @Transactional
    fun archive(id: UUID, hostId: UUID): PropertyView {
        val property = propertyRepository.findById(id)
            ?: throw NotFoundException("Propiedad no encontrada")
        checkOwner(property, hostId)
        property.archive()
        propertyRepository.save(property)
        return assembler.propertyView(property)
    }

    /** Host view: every room, with occupancy and publication state. */
    @Transactional(readOnly = true)
    fun hostDetail(propertyId: UUID, hostId: UUID): PropertyDetailView {
        val property = propertyRepository.findById(propertyId)
            ?: throw NotFoundException("Propiedad no encontrada")
        checkOwner(property, hostId)
        val units = unitRepository.findByProperty(propertyId)
        val occupancy = occupancyPort.occupiedPlacesOf(units.map { it.id })
        return PropertyDetailView(
            property = assembler.propertyView(property),
            host = assembler.hostView(hostId),
            rooms = assembler.unitViews(units, occupancy),
        )
    }

    /** Guest view: published, non-archived property with its published rooms. */
    @Transactional(readOnly = true)
    fun publicDetail(propertyId: UUID): PropertyDetailView? {
        val property = propertyRepository.findById(propertyId) ?: return null
        if (property.archived) return null
        val publishedUnits = unitRepository.findByProperty(propertyId).filter { unit ->
            publicationRepository.findByUnit(unit.id)?.status == PublicationStatus.PUBLICADA
        }
        if (publishedUnits.isEmpty()) return null
        val occupancy = occupancyPort.occupiedPlacesOf(publishedUnits.map { it.id })
        return PropertyDetailView(
            property = assembler.propertyView(property),
            host = assembler.hostView(property.hostId),
            rooms = assembler.unitViews(publishedUnits, occupancy),
        )
    }

    @Transactional
    fun addPhoto(command: AddPhotoCommand): PropertyView {
        val property = requireOwnedProperty(command.ownerId, command.hostId)
        val url = normalizePhotoUrl(command.url)
        val existing = propertyPhotoRepository.findByProperty(property.id)
        val position = command.position ?: (existing.maxOfOrNull { it.position }?.plus(1) ?: 0)
        propertyPhotoRepository.save(
            PropertyPhoto(
                id = UUID.randomUUID(),
                propertyId = property.id,
                url = url,
                position = position,
            ),
        )
        return assembler.propertyView(property)
    }

    @Transactional
    fun deletePhoto(command: DeletePhotoCommand): PropertyView {
        val property = requireOwnedProperty(command.ownerId, command.hostId)
        val photo = propertyPhotoRepository.findById(command.photoId)
            ?: throw NotFoundException("Fotografía no encontrada")
        if (photo.propertyId != property.id) throw ForbiddenException("La fotografía no pertenece a esta propiedad")
        propertyPhotoRepository.delete(photo)
        return assembler.propertyView(property)
    }

    /** Reorders the photos by the given photo ids (unknown ids are ignored). */
    @Transactional
    fun reorderPhotos(propertyId: UUID, hostId: UUID, photoIds: List<UUID>): PropertyView {
        val property = requireOwnedProperty(propertyId, hostId)
        val current = propertyPhotoRepository.findByProperty(propertyId).associateBy { it.id }
        photoIds.forEachIndexed { index, id ->
            current[id]?.let { propertyPhotoRepository.save(it.copy(position = index)) }
        }
        return assembler.propertyView(property)
    }

    private fun requireOwnedProperty(propertyId: UUID, hostId: UUID): Property {
        val property = propertyRepository.findById(propertyId)
            ?: throw NotFoundException("Propiedad no encontrada")
        checkOwner(property, hostId)
        return property
    }

    private fun checkOwner(property: Property, hostId: UUID) {
        if (property.hostId != hostId) throw ForbiddenException("No eres el anfitrión de esta propiedad")
    }

    internal fun normalizePhotoUrl(raw: String): String {
        val url = raw.trim()
        val valid = url.startsWith("/") ||
            url.startsWith("https://") ||
            url.startsWith("http://")
        if (!valid || url.length > 1000) {
            throw InvalidArgumentException("La URL de la fotografía no es válida")
        }
        return url
    }
}
