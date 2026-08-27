package com.coliving.api.accommodation.application.usecase

import com.coliving.api.accommodation.application.dto.CreateUnitCommand
import com.coliving.api.accommodation.application.dto.UnitView
import com.coliving.api.accommodation.application.dto.UpdateUnitCommand
import com.coliving.api.accommodation.domain.model.Property
import com.coliving.api.accommodation.domain.model.Unit
import com.coliving.api.accommodation.domain.repository.PropertyRepository
import com.coliving.api.accommodation.domain.repository.UnitRepository
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.NotFoundException
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class UnitService(
    private val unitRepository: UnitRepository,
    private val propertyRepository: PropertyRepository,
) {

    fun create(command: CreateUnitCommand): UnitView {
        val property = propertyRepository.findById(command.propertyId)
            ?: throw NotFoundException("Property not found")
        checkOwner(property, command.hostId)
        val unit = Unit.create(
            id = UUID.randomUUID(),
            propertyId = command.propertyId,
            name = command.name,
            maxGuests = command.maxGuests,
            bedrooms = command.bedrooms,
            beds = command.beds,
            bathrooms = command.bathrooms,
        )
        unitRepository.save(unit)
        return unit.toView()
    }

    fun update(command: UpdateUnitCommand): UnitView {
        val unit = unitRepository.findById(command.unitId)
            ?: throw NotFoundException("Unit not found")
        val property = propertyRepository.findById(unit.propertyId)
            ?: throw NotFoundException("Property not found")
        checkOwner(property, command.hostId)
        unit.update(
            name = command.name,
            maxGuests = command.maxGuests,
            bedrooms = command.bedrooms,
            beds = command.beds,
            bathrooms = command.bathrooms,
        )
        unitRepository.save(unit)
        return unit.toView()
    }

    fun listByProperty(propertyId: UUID): List<UnitView> =
        unitRepository.findByProperty(propertyId).map { it.toView() }

    private fun checkOwner(property: Property, hostId: UUID) {
        if (property.hostId != hostId) throw ForbiddenException("Not the owner of this property")
    }

    internal fun Unit.toView(): UnitView =
        UnitView(
            id = id,
            propertyId = propertyId,
            name = name,
            maxGuests = maxGuests,
            bedrooms = bedrooms,
            beds = beds,
            bathrooms = bathrooms,
        )
}