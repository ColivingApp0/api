package com.coliving.api.accommodation.application.dto

import com.coliving.api.accommodation.domain.enums.AvailabilityState
import com.coliving.api.accommodation.domain.enums.CancellationPolicy
import com.coliving.api.accommodation.domain.enums.Currency
import com.coliving.api.accommodation.domain.enums.PublicationStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class PropertyView(
    val id: UUID,
    val hostId: UUID,
    val title: String?,
    val description: String?,
    val cityId: UUID?,
    val address: String?,
    val amenities: List<String>,
    val archived: Boolean,
)

data class UnitView(
    val id: UUID,
    val propertyId: UUID,
    val name: String,
    val maxGuests: Int,
    val bedrooms: Int,
    val beds: Int,
    val bathrooms: Int,
)

data class PublicationView(
    val id: UUID,
    val unitId: UUID,
    val title: String,
    val status: PublicationStatus,
)

data class PricingView(
    val publicationId: UUID,
    val basePricePerNight: BigDecimal,
    val currency: Currency,
)

data class RulesView(
    val publicationId: UUID,
    val cancellationPolicy: CancellationPolicy,
    val checkInTime: LocalTime,
    val checkOutTime: LocalTime,
    val minNights: Int,
    val maxNights: Int,
    val houseRules: List<String>,
)

data class DayAvailabilityView(
    val date: LocalDate,
    val state: AvailabilityState,
    val reserved: Boolean,
)

data class PublicationDetailView(
    val publication: PublicationView,
    val property: PropertyView,
    val unit: UnitView,
    val pricing: PricingView?,
    val rules: RulesView?,
)