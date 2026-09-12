package com.coliving.api.community.infrastructure.provider

import com.coliving.api.accommodation.application.query.PropertyCommunityQuery
import com.coliving.api.community.application.port.out.PropertyCommunityPort
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Adapter over accommodation's [PropertyCommunityQuery]: community resolves the
 * property owner, the host's properties and the property's units without
 * owning or reading accommodation data (RF-060).
 */
@Component
class AccommodationPropertyProvider(
    private val propertyCommunityQuery: PropertyCommunityQuery,
) : PropertyCommunityPort {

    override fun hostOfProperty(propertyId: UUID): UUID? =
        propertyCommunityQuery.hostOfProperty(propertyId)

    override fun propertyIdsOfHost(hostId: UUID): List<UUID> =
        propertyCommunityQuery.propertyIdsOfHost(hostId)

    override fun unitIdsOfProperty(propertyId: UUID): List<UUID> =
        propertyCommunityQuery.unitIdsOfProperty(propertyId)
}