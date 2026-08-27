package com.coliving.api.accommodation.application.dto

import com.coliving.api.accommodation.domain.enums.CancellationPolicy
import com.coliving.api.accommodation.domain.enums.Currency
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/** RF: host creates a property. */
data class CreatePropertyCommand(
    val hostId: UUID,
    val title: String,
    val description: String? = null,
    val cityId: UUID? = null,
    val address: String? = null,
    val amenities: List<String> = emptyList(),
)

data class UpdatePropertyCommand(
    val propertyId: UUID,
    val hostId: UUID,
    val title: String?,
    val description: String?,
    val cityId: UUID?,
    val address: String?,
    val amenities: List<String>,
)

data class CreateUnitCommand(
    val propertyId: UUID,
    val hostId: UUID,
    val name: String,
    val maxGuests: Int,
    val bedrooms: Int,
    val beds: Int,
    val bathrooms: Int,
)

data class UpdateUnitCommand(
    val unitId: UUID,
    val hostId: UUID,
    val name: String,
    val maxGuests: Int,
    val bedrooms: Int,
    val beds: Int,
    val bathrooms: Int,
)

data class CreatePublicationCommand(
    val unitId: UUID,
    val hostId: UUID,
    val title: String,
)

data class ConfigurePricingCommand(
    val publicationId: UUID,
    val hostId: UUID,
    val basePricePerNight: BigDecimal,
    val currency: Currency,
)

data class ConfigureRulesCommand(
    val publicationId: UUID,
    val hostId: UUID,
    val cancellationPolicy: CancellationPolicy,
    val checkInTime: LocalTime,
    val checkOutTime: LocalTime,
    val minNights: Int,
    val maxNights: Int,
    val houseRules: List<String> = emptyList(),
)

/** Host manual block/unblock of a date range (reservation_id = null). */
data class BlockDatesCommand(
    val unitId: UUID,
    val hostId: UUID,
    val from: LocalDate,
    val to: LocalDate,
)

data class UnblockDatesCommand(
    val unitId: UUID,
    val hostId: UUID,
    val from: LocalDate,
    val to: LocalDate,
)