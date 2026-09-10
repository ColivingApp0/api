package com.coliving.api.accommodation.application.query

import java.util.UUID

/**
 * Read contract that the `community` bounded context consumes (through an
 * adapter) to resolve the property an activity belongs to and who owns it.
 * Mirrors identity's `UserVerificationQuery` and this context's
 * `UnitBookingQuery`: the providing context owns both the data and the
 * projection, so community never reads accommodation tables.
 */
interface PropertyCommunityQuery {

    /**
     * Host who owns [propertyId], or null when the property does not exist.
     * Community uses it to authorize `RF-060` (only the host creates the
     * activities of their property).
     */
    fun hostOfProperty(propertyId: UUID): UUID?

    /** Properties owned by [hostId], for the host's community listing. */
    fun propertyIdsOfHost(hostId: UUID): List<UUID>

    /**
     * Units of [propertyId]. Community resolves the property's residents
     * through booking (RF-061) using these unit ids.
     */
    fun unitIdsOfProperty(propertyId: UUID): List<UUID>
}