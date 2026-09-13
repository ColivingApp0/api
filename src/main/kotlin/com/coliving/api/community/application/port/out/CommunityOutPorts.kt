package com.coliving.api.community.application.port.out

import java.util.UUID

/**
 * Property read contract consumed from `accommodation` (RF-060): who owns the
 * property, which properties a host manages and which units it has. Implemented
 * by an adapter delegating to accommodation's PropertyCommunityQuery.
 */
interface PropertyCommunityPort {

    fun hostOfProperty(propertyId: UUID): UUID?

    fun propertyIdsOfHost(hostId: UUID): List<UUID>

    fun unitIdsOfProperty(propertyId: UUID): List<UUID>
}

/**
 * Residency read contract consumed from `booking` (RF-061): the residents of a
 * property are the guests whose reservation over any of its units is confirmed.
 */
interface CommunityResidencyPort {

    fun residentIdsOfProperty(propertyId: UUID): List<UUID>
}

/**
 * Consent read contract consumed from `identity` (RF-062 / RN-05): aggregated
 * community information is only exposed when there is sufficient consent.
 */
interface CommunityConsentPort {

    fun hasDataSharingConsent(userId: UUID): Boolean
}