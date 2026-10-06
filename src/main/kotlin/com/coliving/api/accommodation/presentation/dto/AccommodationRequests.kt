package com.coliving.api.accommodation.presentation.dto

import com.coliving.api.accommodation.domain.enums.BathroomType
import com.coliving.api.accommodation.domain.enums.Currency
import com.coliving.api.accommodation.domain.enums.PriceUnit
import com.coliving.api.accommodation.domain.enums.ReviewDecision
import com.coliving.api.accommodation.domain.enums.RoomType
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.UUID

data class CreatePropertyRequest(
    @field:NotBlank(message = "El nombre es obligatorio")
    val title: String = "",
    val description: String? = null,
    val cityId: UUID? = null,
    val cityName: String? = null,
    val neighborhood: String? = null,
    val address: String? = null,
    val amenities: List<String> = emptyList(),
)

data class UpdatePropertyRequest(
    val title: String? = null,
    val description: String? = null,
    val cityId: UUID? = null,
    val cityName: String? = null,
    val neighborhood: String? = null,
    val address: String? = null,
    val amenities: List<String>? = null,
)

data class CreateUnitRequest(
    @field:NotBlank(message = "El nombre es obligatorio")
    val name: String = "",
    val description: String? = null,
    @field:Min(1, message = "La capacidad debe ser al menos 1")
    val maxGuests: Int = 1,
    @field:DecimalMin(value = "0.01", message = "El precio mensual debe ser positivo")
    val monthlyPrice: BigDecimal? = null,
    val currency: Currency = Currency.COP,
    val priceUnit: PriceUnit = PriceUnit.POR_HABITACION,
    val roomType: RoomType = RoomType.INDIVIDUAL,
    val bathroomType: BathroomType = BathroomType.COMPARTIDO,
    val airConditioning: Boolean = false,
    val fan: Boolean = false,
    val commonAreas: List<String> = emptyList(),
    val typeCode: UUID? = null,
    val accessibilityCodes: List<UUID> = emptyList(),
    val sortOrder: Int = 0,
)

data class UpdateUnitRequest(
    @field:NotBlank(message = "El nombre es obligatorio")
    val name: String = "",
    val description: String? = null,
    @field:Min(1, message = "La capacidad debe ser al menos 1")
    val maxGuests: Int = 1,
    @field:DecimalMin(value = "0.01", message = "El precio mensual debe ser positivo")
    val monthlyPrice: BigDecimal? = null,
    val currency: Currency = Currency.COP,
    val priceUnit: PriceUnit = PriceUnit.POR_HABITACION,
    val roomType: RoomType = RoomType.INDIVIDUAL,
    val bathroomType: BathroomType = BathroomType.COMPARTIDO,
    val airConditioning: Boolean = false,
    val fan: Boolean = false,
    val commonAreas: List<String> = emptyList(),
    val typeCode: UUID? = null,
    val accessibilityCodes: List<UUID>? = null,
    val sortOrder: Int? = null,
)

data class SetAvailabilityRequest(
    @field:NotNull(message = "enabled es obligatorio")
    val enabled: Boolean? = null,
)

data class AddPhotoRequest(
    @field:NotBlank(message = "La URL de la fotografía es obligatoria")
    @field:Size(max = 1000, message = "La URL no puede superar 1000 caracteres")
    val url: String,
    val position: Int? = null,
)

data class ReorderPhotosRequest(
    @field:NotNull(message = "photoIds es obligatorio")
    val photoIds: List<UUID> = emptyList(),
)

data class CreatePublicationRequest(
    @field:NotBlank(message = "El título es obligatorio")
    val title: String = "",
    val services: List<UUID> = emptyList(),
    val applicableRuleCodes: List<UUID> = emptyList(),
)

data class ConfigureCatalogReferencesRequest(
    @field:NotNull(message = "services es obligatorio")
    val services: List<UUID>? = null,
    @field:NotNull(message = "applicableRuleCodes es obligatorio")
    val applicableRuleCodes: List<UUID>? = null,
)

data class ConfigurePricingRequest(
    @field:NotNull(message = "monthlyPrice es obligatorio")
    @field:DecimalMin(value = "0.01", message = "El precio mensual debe ser positivo")
    val monthlyPrice: BigDecimal? = null,
    @field:NotNull(message = "currency es obligatorio")
    val currency: Currency? = null,
    val priceUnit: PriceUnit = PriceUnit.POR_HABITACION,
)

data class ReviewPublicationRequest(
    @field:NotNull(message = "decision es obligatorio")
    val decision: ReviewDecision? = null,
    @field:Size(max = 1000, message = "La nota no puede superar 1000 caracteres")
    val note: String? = null,
)

data class ReportPublicationRequest(
    @field:NotBlank(message = "El motivo es obligatorio")
    @field:Size(max = 1000, message = "El motivo no puede superar 1000 caracteres")
    val reason: String,
)
