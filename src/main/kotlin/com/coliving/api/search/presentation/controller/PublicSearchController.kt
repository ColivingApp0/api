package com.coliving.api.search.presentation.controller

import com.coliving.api.search.application.dto.ListingSort
import com.coliving.api.search.application.dto.ListingView
import com.coliving.api.search.application.dto.SearchListingsCommand
import com.coliving.api.search.application.usecase.SearchListingsService
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Public search endpoint for any authenticated user (RF-030, RF-032). Filters
 * are combinable and optional; ordering is always deterministic.
 */
@RestController
@RequestMapping("/api/v1/search")
class PublicSearchController(
    private val searchListingsService: SearchListingsService,
) {

    @GetMapping("/listings")
    fun search(
        @RequestParam("cityId") cityId: UUID?,
        @RequestParam("minPrice") minPrice: BigDecimal?,
        @RequestParam("maxPrice") maxPrice: BigDecimal?,
        @RequestParam("currency") currency: String?,
        @RequestParam("availableFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) availableFrom: LocalDate?,
        @RequestParam("availableTo") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) availableTo: LocalDate?,
        @RequestParam("sort", defaultValue = "RECENT") sort: ListingSort,
    ): List<ListingView> = searchListingsService.search(
        SearchListingsCommand(
            cityId = cityId,
            minPrice = minPrice,
            maxPrice = maxPrice,
            currency = currency,
            availableFrom = availableFrom,
            availableTo = availableTo,
            sort = sort,
        ),
    )
}
