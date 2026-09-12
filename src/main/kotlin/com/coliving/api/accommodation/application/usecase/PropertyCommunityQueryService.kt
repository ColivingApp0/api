package com.coliving.api.accommodation.application.usecase

import com.coliving.api.accommodation.application.query.PropertyCommunityQuery
import com.coliving.api.accommodation.domain.repository.PropertyRepository
import com.coliving.api.accommodation.domain.repository.UnitRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read-side implementation of [PropertyCommunityQuery]: pure projection over the
 * property and unit aggregates — no mutation and no business decision
 * (deciding who may take part in an activity is community's job).
 */
@Service
class PropertyCommunityQueryService(
    private val propertyRepository: PropertyRepository,
    private val unitRepository: UnitRepository,
) : PropertyCommunityQuery {

    @Transactional(readOnly = true)
    override fun hostOfProperty(propertyId: UUID): UUID? =
        propertyRepository.findById(propertyId)?.hostId

    @Transactional(readOnly = true)
    override fun propertyIdsOfHost(hostId: UUID): List<UUID> =
        propertyRepository.findByHost(hostId).map { it.id }

    @Transactional(readOnly = true)
    override fun unitIdsOfProperty(propertyId: UUID): List<UUID> =
        unitRepository.findByProperty(propertyId).map { it.id }
}