package com.coliving.api.accommodation.application.usecase

import com.coliving.api.accommodation.application.dto.AddPhotoCommand
import com.coliving.api.accommodation.application.dto.CreateUnitCommand
import com.coliving.api.accommodation.application.dto.SetUnitAvailabilityCommand
import com.coliving.api.accommodation.application.dto.UnitView
import com.coliving.api.accommodation.application.dto.UpdateUnitCommand
import com.coliving.api.accommodation.application.port.out.HostVerificationPort
import com.coliving.api.accommodation.application.port.out.UnitOccupancyPort
import com.coliving.api.accommodation.domain.enums.PublicationStatus
import com.coliving.api.accommodation.domain.model.Property
import com.coliving.api.accommodation.domain.model.Publication
import com.coliving.api.accommodation.domain.model.Unit
import com.coliving.api.accommodation.domain.model.UnitPhoto
import com.coliving.api.accommodation.domain.repository.PropertyRepository
import com.coliving.api.accommodation.domain.repository.PublicationRepository
import com.coliving.api.accommodation.domain.repository.UnitPhotoRepository
import com.coliving.api.accommodation.domain.repository.UnitRepository
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.InvalidArgumentException
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UnitService(
    private val unitRepository: UnitRepository,
    private val propertyRepository: PropertyRepository,
    private val publicationRepository: PublicationRepository,
    private val unitPhotoRepository: UnitPhotoRepository,
    private val occupancyPort: UnitOccupancyPort,
    private val hostVerificationPort: HostVerificationPort,
    private val assembler: AccommodationViewAssembler,
) {

    @Transactional
    fun create(command: CreateUnitCommand): UnitView {
        val property = propertyRepository.findById(command.propertyId)
            ?: throw NotFoundException("Propiedad no encontrada")
        checkOwner(property, command.hostId)
        if (property.archived) throw ConflictException("La propiedad está archivada")
        val unit = Unit.create(
            id = UUID.randomUUID(),
            propertyId = command.propertyId,
            name = command.name,
            description = command.description,
            maxGuests = command.maxGuests,
            monthlyPrice = command.monthlyPrice,
            currency = command.currency,
            priceUnit = command.priceUnit,
            roomType = command.roomType,
            bathroomType = command.bathroomType,
            airConditioning = command.airConditioning,
            fan = command.fan,
            commonAreas = command.commonAreas,
            typeCode = command.typeCode,
            accessibilityCodes = command.accessibilityCodes,
            sortOrder = command.sortOrder,
        )
        unitRepository.save(unit)
        publicationRepository.save(
            Publication.create(
                id = UUID.randomUUID(),
                unitId = unit.id,
                title = unit.name,
                now = Instant.now(),
            ),
        )
        return assembler.unitView(unit, occupied = 0)
    }

    @Transactional
    fun update(command: UpdateUnitCommand): UnitView {
        val unit = unitRepository.findById(command.unitId)
            ?: throw NotFoundException("Habitación no encontrada")
        val property = propertyRepository.findById(unit.propertyId)
            ?: throw NotFoundException("Propiedad no encontrada")
        checkOwner(property, command.hostId)
        val occupied = occupancyPort.occupiedPlacesOf(listOf(unit.id))[unit.id] ?: 0
        unit.update(
            name = command.name,
            description = command.description,
            maxGuests = command.maxGuests,
            monthlyPrice = command.monthlyPrice,
            currency = command.currency,
            priceUnit = command.priceUnit,
            roomType = command.roomType,
            bathroomType = command.bathroomType,
            airConditioning = command.airConditioning,
            fan = command.fan,
            commonAreas = command.commonAreas,
            typeCode = command.typeCode,
            accessibilityCodes = command.accessibilityCodes,
            sortOrder = command.sortOrder,
            occupiedPlaces = occupied,
        )
        unitRepository.save(unit)
        // Keep the listing title aligned with the room name.
        publicationRepository.findByUnit(unit.id)?.let { publication ->
            publication.rename(unit.name, Instant.now())
            publicationRepository.save(publication)
        }
        return assembler.unitView(unit, occupied)
    }

    fun listByProperty(propertyId: UUID, hostId: UUID? = null): List<UnitView> {
        val property = propertyRepository.findById(propertyId)
            ?: throw NotFoundException("Propiedad no encontrada")
        if (hostId != null) checkOwner(property, hostId)
        val units = unitRepository.findByProperty(propertyId)
        val occupancy = occupancyPort.occupiedPlacesOf(units.map { it.id })
        return assembler.unitViews(units, occupancy)
    }

    fun findUnit(unitId: UUID, hostId: UUID): UnitView {
        val unit = requireOwnedUnit(unitId, hostId)
        return assembler.unitView(unit)
    }

    /**
     * Commercial availability switch: publishing requires a verified host and a
     * monthly price; pausing always works, even when the room still has free
     * places.
     */
    @Transactional
    fun setAvailability(command: SetUnitAvailabilityCommand): UnitView {
        val unit = unitRepository.findById(command.unitId)
            ?: throw NotFoundException("Habitación no encontrada")
        val property = propertyRepository.findById(unit.propertyId)
            ?: throw NotFoundException("Propiedad no encontrada")
        checkOwner(property, command.hostId)
        val publication = publicationRepository.findByUnit(unit.id)
            ?: throw NotFoundException("Publicación de la habitación no encontrada")
        if (command.enabled) {
            if (unit.monthlyPrice == null) {
                throw ConflictException("Define el precio mensual antes de habilitar la habitación")
            }
            if (!hostVerificationPort.isVerifiedHost(command.hostId)) {
                throw ConflictException("Verifica tu identidad para publicar habitaciones")
            }
            when (publication.status) {
                PublicationStatus.BORRADOR,
                PublicationStatus.PAUSADA,
                PublicationStatus.OCULTA,
                -> publication.publish()
                PublicationStatus.PUBLICADA -> { /* already enabled: idempotent */ }
                else -> throw ConflictException(
                    "No se puede habilitar una habitación en estado ${publication.status}",
                )
            }
        } else {
            when (publication.status) {
                PublicationStatus.PUBLICADA -> publication.pause()
                PublicationStatus.BORRADOR,
                PublicationStatus.PAUSADA,
                PublicationStatus.OCULTA,
                -> { /* already not accepting requests: idempotent */ }
                else -> throw ConflictException(
                    "No se puede pausar una habitación en estado ${publication.status}",
                )
            }
        }
        publication.touch(Instant.now())
        publicationRepository.save(publication)
        return assembler.unitView(unit)
    }

    @Transactional
    fun addPhoto(command: AddPhotoCommand): UnitView {
        val unit = requireOwnedUnit(command.ownerId, command.hostId)
        val url = normalizePhotoUrl(command.url)
        val existing = unitPhotoRepository.findByUnit(unit.id)
        val position = command.position ?: (existing.maxOfOrNull { it.position }?.plus(1) ?: 0)
        unitPhotoRepository.save(UnitPhoto(UUID.randomUUID(), unit.id, url, position))
        return assembler.unitView(unit)
    }

    @Transactional
    fun deletePhoto(unitId: UUID, photoId: UUID, hostId: UUID): UnitView {
        val unit = requireOwnedUnit(unitId, hostId)
        val photo = unitPhotoRepository.findById(photoId)
            ?: throw NotFoundException("Fotografía no encontrada")
        if (photo.unitId != unit.id) throw ForbiddenException("La fotografía no pertenece a esta habitación")
        unitPhotoRepository.delete(photo)
        return assembler.unitView(unit)
    }

    @Transactional
    fun reorderPhotos(unitId: UUID, hostId: UUID, photoIds: List<UUID>): UnitView {
        val unit = requireOwnedUnit(unitId, hostId)
        val current = unitPhotoRepository.findByUnit(unitId).associateBy { it.id }
        photoIds.forEachIndexed { index, id ->
            current[id]?.let { unitPhotoRepository.save(it.copy(position = index)) }
        }
        return assembler.unitView(unit)
    }

    private fun requireOwnedUnit(unitId: UUID, hostId: UUID): Unit {
        val unit = unitRepository.findById(unitId)
            ?: throw NotFoundException("Habitación no encontrada")
        val property = propertyRepository.findById(unit.propertyId)
            ?: throw NotFoundException("Propiedad no encontrada")
        checkOwner(property, hostId)
        return unit
    }

    private fun checkOwner(property: Property, hostId: UUID) {
        if (property.hostId != hostId) throw ForbiddenException("No eres el anfitrión de esta propiedad")
    }

    private fun normalizePhotoUrl(raw: String): String {
        val url = raw.trim()
        val valid = url.startsWith("/") || url.startsWith("https://") || url.startsWith("http://")
        if (!valid || url.length > 1000) {
            throw InvalidArgumentException("La URL de la fotografía no es válida")
        }
        return url
    }
}
