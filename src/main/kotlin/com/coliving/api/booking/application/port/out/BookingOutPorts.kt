package com.coliving.api.booking.application.port.out

import com.coliving.api.accommodation.domain.enums.Currency
import com.coliving.api.accommodation.domain.enums.PriceUnit
import java.math.BigDecimal
import java.util.UUID

/**
 * Catalog snapshot of a room, read from `accommodation`. Non-null only when the
 * room and its publication exist; [BookableUnit.status] must be PUBLICADA for
 * the room to admit requests (RN-03). The monthly price is frozen in the
 * reservation snapshot (RN-07).
 */
data class BookableUnit(
    val unitId: UUID,
    val propertyId: UUID,
    val publicationId: UUID,
    val status: String,
    val unitName: String,
    val propertyTitle: String,
    val cityName: String?,
    val neighborhood: String?,
    val photoUrl: String?,
    val capacity: Int,
    val monthlyPrice: BigDecimal?,
    val currency: Currency,
    val priceUnit: PriceUnit,
    val hostId: UUID,
)

interface UnitCatalogPort {

    fun findBookableUnit(unitId: UUID): BookableUnit?

    /** Same snapshot with a pessimistic write lock on the room row. */
    fun lockBookableUnit(unitId: UUID): BookableUnit?

    fun unitIdsOfHost(hostId: UUID): List<UUID>

    fun isHostOfUnit(unitId: UUID, hostId: UUID): Boolean

    /** Owner of the room, or null when the room does not exist (messaging). */
    fun hostOfUnit(unitId: UUID): UUID?
}

/**
 * Guest gate (RF-040): a guest may request a place only with COMPLETO
 * verification. Implemented by an adapter to identity's UserVerificationQuery.
 */
interface GuestVerificationPort {

    fun hasMinimumVerification(guestUserId: UUID): Boolean
}
