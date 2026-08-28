package com.coliving.api.accommodation.presentation.dto

import com.coliving.api.accommodation.domain.enums.CancellationPolicy
import com.coliving.api.accommodation.domain.enums.Currency
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalTime
import java.util.UUID

data class CreatePropertyRequest(
    @field:NotBlank(message = "title is required")
    val title: String = "",

    val description: String? = null,
    val cityId: UUID? = null,
    val address: String? = null,
    val amenities: List<String> = emptyList(),
)

data class UpdatePropertyRequest(
    val title: String? = null,
    val description: String? = null,
    val cityId: UUID? = null,
    val address: String? = null,
    val amenities: List<String>? = null,
)

data class CreateUnitRequest(
    @field:NotBlank(message = "name is required")
    val name: String = "",

    @field:Min(1, message = "maxGuests must be at least 1")
    val maxGuests: Int = 1,

    @field:Min(0, message = "bedrooms must not be negative")
    val bedrooms: Int = 0,

    @field:Min(0, message = "beds must not be negative")
    val beds: Int = 1,

    @field:Min(0, message = "bathrooms must not be negative")
    val bathrooms: Int = 1,
)

data class UpdateUnitRequest(
    @field:NotBlank(message = "name is required")
    val name: String = "",

    @field:Min(1, message = "maxGuests must be at least 1")
    val maxGuests: Int = 1,

    @field:Min(0, message = "bedrooms must not be negative")
    val bedrooms: Int = 0,

    @field:Min(0, message = "beds must not be negative")
    val beds: Int = 1,

    @field:Min(0, message = "bathrooms must not be negative")
    val bathrooms: Int = 1,
)

data class CreatePublicationRequest(
    @field:NotBlank(message = "title is required")
    val title: String = "",
)

data class ConfigurePricingRequest(
    @field:NotNull(message = "basePricePerNight is required")
    @field:DecimalMin(value = "0.01", message = "basePricePerNight must be positive")
    val basePricePerNight: BigDecimal? = null,

    @field:NotNull(message = "currency is required")
    val currency: Currency? = null,
)

data class ConfigureRulesRequest(
    @field:NotNull(message = "cancellationPolicy is required")
    val cancellationPolicy: CancellationPolicy? = null,

    @field:NotNull(message = "checkInTime is required")
    val checkInTime: LocalTime? = null,

    @field:NotNull(message = "checkOutTime is required")
    val checkOutTime: LocalTime? = null,

    @field:Min(1, message = "minNights must be at least 1")
    val minNights: Int = 1,

    @field:Min(1, message = "maxNights must be at least 1")
    val maxNights: Int = 365,

    val houseRules: List<String> = emptyList(),
)