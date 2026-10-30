package com.coliving.api.accommodation.application.dto

import com.coliving.api.accommodation.domain.enums.BathroomType
import com.coliving.api.accommodation.domain.enums.Currency
import com.coliving.api.accommodation.domain.enums.PriceUnit
import com.coliving.api.accommodation.domain.enums.PublicationStatus
import com.coliving.api.accommodation.domain.enums.RoomType
import java.math.BigDecimal
import java.util.UUID

/** Monthly-rental response contracts. Money is always monthly and explicit. */
data class PhotoView(
    val id: UUID,
    val url: String,
    val position: Int,
)

data class PropertyView(
    val id: UUID,
    val hostId: UUID,
    val title: String?,
    val description: String?,
    val cityId: UUID?,
    val cityName: String?,
    val neighborhood: String?,
    val address: String?,
    val amenities: List<String>,
    val archived: Boolean,
    val photos: List<PhotoView> = emptyList(),
)

data class PublicationView(
    val id: UUID,
    val unitId: UUID,
    val title: String,
    val status: PublicationStatus,
    val services: Set<UUID> = emptySet(),
    val applicableRuleCodes: Set<UUID> = emptySet(),
)

/** Monthly price of a room (kept for clients that still use the pricing path). */
data class PricingView(
    val publicationId: UUID,
    val monthlyPrice: BigDecimal?,
    val currency: Currency,
    val priceUnit: PriceUnit,
)

data class UnitView(
    val id: UUID,
    val propertyId: UUID,
    val name: String,
    val description: String?,
    val maxGuests: Int,
    val monthlyPrice: BigDecimal?,
    val currency: Currency,
    val priceUnit: PriceUnit,
    val roomType: RoomType,
    val bathroomType: BathroomType,
    val airConditioning: Boolean,
    val fan: Boolean,
    val commonAreas: List<String>,
    val typeCode: UUID? = null,
    val accessibilityCodes: Set<UUID> = emptySet(),
    val sortOrder: Int = 0,
    val photos: List<PhotoView> = emptyList(),
    val publication: PublicationView? = null,
    val occupiedPlaces: Int = 0,
    val availablePlaces: Int = 0,
    val published: Boolean = false,
    val effectiveAvailable: Boolean = false,
    val availabilityLabel: String = "No disponible",
)

/** Public profile card used for hosts and — with consent — roommates. */
data class PublicUserView(
    val userId: UUID,
    val name: String?,
    val avatarUrl: String?,
)

data class OccupancyView(
    val capacity: Int,
    val occupiedPlaces: Int,
    val availablePlaces: Int,
    val published: Boolean,
    val effectiveAvailable: Boolean,
    val label: String,
)

data class PropertyDetailView(
    val property: PropertyView,
    val host: PublicUserView?,
    val rooms: List<UnitView>,
)

data class PublicationDetailView(
    val publication: PublicationView,
    val property: PropertyView,
    val unit: UnitView,
    val host: PublicUserView?,
    val occupancy: OccupancyView,
    val propertyPhotos: List<PhotoView> = emptyList(),
)

/** A roommate of a shared room, only when their profile sharing is authorized. */
data class RoommateView(
    val userId: UUID,
    val name: String,
    val avatarUrl: String?,
)
