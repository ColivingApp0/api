package com.coliving.api.accommodation.application.dto

import com.coliving.api.accommodation.domain.enums.Currency
import java.math.BigDecimal
import java.util.UUID

/**
 * Everything the `booking` context needs to know about a unit in order to
 * validate and price a reservation request. Exposed through
 * [com.coliving.api.accommodation.application.query.UnitBookingQuery].
 *
 * `basePricePerNight`/`currency`/`cancellationPolicy` are the values a booking
 * must snapshot (RN-07): later changes here never alter an existing reservation.
 */
data class UnitBookingInfo(
    val unitId: UUID,
    val publicationId: UUID,
    val publicationStatus: String,
    val maxGuests: Int,
    val basePricePerNight: BigDecimal,
    val currency: Currency,
    val cancellationPolicy: String?,
)
