package com.coliving.api.accommodation.application.usecase

import com.coliving.api.accommodation.application.dto.CreatePropertyCommand
import com.coliving.api.accommodation.application.dto.PropertyView
import com.coliving.api.accommodation.application.dto.UpdatePropertyCommand
import com.coliving.api.accommodation.domain.model.Property
import com.coliving.api.accommodation.domain.repository.PropertyRepository
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.NotFoundException
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class PropertyService(
    private val propertyRepository: PropertyRepository,
) {

    fun create(command: CreatePropertyCommand): PropertyView {
        if (command.title.isBlank()) throw com.coliving.api.shared.error.InvalidArgumentException("title is required")
        val property = Property.create(
            id = UUID.randomUUID(),
            hostId = command.hostId,
            title = command.title,
            description = command.description,
            cityId = command.cityId,
            address = command.address,
            amenities = command.amenities,
        )
        propertyRepository.save(property)
        return property.toView()
    }

    fun findById(id: UUID): PropertyView? =
        propertyRepository.findById(id)?.toView()

    fun listByHost(hostId: UUID): List<PropertyView> =
        propertyRepository.findByHost(hostId).map { it.toView() }

    fun update(command: UpdatePropertyCommand): PropertyView {
        val property = propertyRepository.findById(command.propertyId)
            ?: throw NotFoundException("Property not found")
        checkOwner(property, command.hostId)
        property.update(
            title = command.title ?: property.title,
            description = if (command.description != null) command.description else property.description,
            cityId = if (command.cityId != null) command.cityId else property.cityId,
            address = if (command.address != null) command.address else property.address,
            amenities = command.amenities,
        )
        propertyRepository.save(property)
        return property.toView()
    }

    fun archive(id: UUID, hostId: UUID): PropertyView {
        val property = propertyRepository.findById(id)
            ?: throw NotFoundException("Property not found")
        checkOwner(property, hostId)
        property.archive()
        propertyRepository.save(property)
        return property.toView()
    }

    private fun checkOwner(property: Property, hostId: UUID) {
        if (property.hostId != hostId) throw ForbiddenException("Not the owner of this property")
    }

    internal fun Property.toView(): PropertyView =
        PropertyView(
            id = id,
            hostId = hostId,
            title = title,
            description = description,
            cityId = cityId,
            address = address,
            amenities = amenities,
            archived = archived,
        )
}