package com.coliving.api.accommodation.application.dto

import com.coliving.api.accommodation.domain.enums.BathroomType
import com.coliving.api.accommodation.domain.enums.Currency
import com.coliving.api.accommodation.domain.enums.PriceUnit
import com.coliving.api.accommodation.domain.enums.RoomType
import java.math.BigDecimal
import java.util.UUID

/** Commands of the monthly room-rental host flow. */

data class CreatePropertyCommand(
    val hostId: UUID,
    val title: String,
    val description: String? = null,
    val cityId: UUID? = null,
    val cityName: String? = null,
    val neighborhood: String? = null,
    val address: String? = null,
    val amenities: List<String> = emptyList(),
)

data class UpdatePropertyCommand(
    val propertyId: UUID,
    val hostId: UUID,
    val title: String?,
    val description: String?,
    val cityId: UUID?,
    val cityName: String?,
    val neighborhood: String?,
    val address: String?,
    val amenities: List<String>,
)

data class CreateUnitCommand(
    val propertyId: UUID,
    val hostId: UUID,
    val name: String,
    val description: String? = null,
    val maxGuests: Int,
    val monthlyPrice: BigDecimal? = null,
    val currency: Currency = Currency.COP,
    val priceUnit: PriceUnit = PriceUnit.POR_HABITACION,
    val roomType: RoomType = RoomType.INDIVIDUAL,
    val bathroomType: BathroomType = BathroomType.COMPARTIDO,
    val airConditioning: Boolean = false,
    val fan: Boolean = false,
    val commonAreas: List<String> = emptyList(),
    val typeCode: UUID? = null,
    val accessibilityCodes: Set<UUID> = emptySet(),
    val sortOrder: Int = 0,
)

data class UpdateUnitCommand(
    val unitId: UUID,
    val hostId: UUID,
    val name: String,
    val description: String? = null,
    val maxGuests: Int,
    val monthlyPrice: BigDecimal? = null,
    val currency: Currency = Currency.COP,
    val priceUnit: PriceUnit = PriceUnit.POR_HABITACION,
    val roomType: RoomType = RoomType.INDIVIDUAL,
    val bathroomType: BathroomType = BathroomType.COMPARTIDO,
    val airConditioning: Boolean = false,
    val fan: Boolean = false,
    val commonAreas: List<String> = emptyList(),
    val typeCode: UUID? = null,
    val accessibilityCodes: Set<UUID>? = null,
    val sortOrder: Int? = null,
)

data class CreatePublicationCommand(
    val unitId: UUID,
    val hostId: UUID,
    val title: String,
    val services: Set<UUID> = emptySet(),
    val applicableRuleCodes: Set<UUID> = emptySet(),
)

data class ConfigurePricingCommand(    val publicationId: UUID,
    val hostId: UUID,
    val monthlyPrice: BigDecimal,
    val currency: Currency,
    val priceUnit: PriceUnit = PriceUnit.POR_HABITACION,
)

data class ConfigureCatalogReferencesCommand(
    val publicationId: UUID,
    val hostId: UUID,
    val services: Set<UUID>,
    val applicableRuleCodes: Set<UUID>,
)

/** Adds a photograph to a property or room. */
data class AddPhotoCommand(
    val ownerId: UUID,
    val hostId: UUID,
    val url: String,
    val position: Int? = null,
)

data class DeletePhotoCommand(
    val ownerId: UUID,
    val photoId: UUID,
    val hostId: UUID,
)

/** Enables (publishes) or pauses a room's commercial availability. */
data class SetUnitAvailabilityCommand(
    val unitId: UUID,
    val hostId: UUID,
    val enabled: Boolean,
)
