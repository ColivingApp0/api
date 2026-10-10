package com.coliving.api.search.presentation.controller

import com.coliving.api.search.application.dto.ListingSort
import com.coliving.api.search.application.dto.ListingView
import com.coliving.api.search.application.dto.SearchListingsCommand
import com.coliving.api.search.application.usecase.SearchListingsService
import io.swagger.v3.oas.annotations.tags.Tag
import java.math.BigDecimal
import java.util.UUID
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Public room search (RF-030, RF-032). Filters are combinable and optional;
 * ordering is always deterministic. Prices are monthly.
 */
@Tag(
    name = "Search",
    description = "Search of published rooms with monthly price, location, capacity and room features (RF-030, RF-032).",
)
@RestController
@RequestMapping("/api/v1/search")
class PublicSearchController(
    private val searchListingsService: SearchListingsService,
) {

    @GetMapping("/listings")
    fun search(
        @RequestParam("q") q: String?,
        @RequestParam("cityId") cityId: UUID?,
        @RequestParam("minPrice") minPrice: BigDecimal?,
        @RequestParam("maxPrice") maxPrice: BigDecimal?,
        @RequestParam("currency") currency: String?,
        @RequestParam("sort", defaultValue = "RECENT") sort: ListingSort,
        @RequestParam("minCapacity") minCapacity: Int?,
        @RequestParam("availableOnly", defaultValue = "true") availableOnly: Boolean,
        @RequestParam("airConditioning") airConditioning: Boolean?,
        @RequestParam("fan") fan: Boolean?,
        @RequestParam("bathroomType") bathroomType: String?,
        @RequestParam("roomType") roomType: String?,
        @RequestParam("propertyId") propertyId: UUID?,
        @RequestParam("services") services: String?,
        @RequestParam("accessibilityCode") accessibilityCode: UUID?,
    ): List<ListingView> = searchListingsService.search(
        SearchListingsCommand(
            q = q,
            cityId = cityId,
            minPrice = minPrice,
            maxPrice = maxPrice,
            currency = currency,
            sort = sort,
            minCapacity = minCapacity,
            availableOnly = availableOnly,
            airConditioning = airConditioning,
            fan = fan,
            bathroomType = bathroomType,
            roomType = roomType,
            propertyId = propertyId,
            services = services?.split(",")?.map { UUID.fromString(it.trim()) }?.toSet(),
            accessibilityCode = accessibilityCode,
        ),
    )
}
