package com.coliving.api.accommodation.application.dto

import com.coliving.api.accommodation.domain.enums.Currency
import com.coliving.api.accommodation.domain.enums.PriceUnit
import java.math.BigDecimal
import java.util.UUID

/**
 * Everything the `booking` context needs to validate and price a monthly room
 * request. Exposed through [com.coliving.api.accommodation.application.query.UnitBookingQuery].
 */
data class UnitBookingInfo(
    val unitId: UUID,
    val propertyId: UUID,
    val publicationId: UUID,
    val publicationStatus: String,
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
