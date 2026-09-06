package com.coliving.api.search.application

import com.coliving.api.search.application.dto.ListingSort
import com.coliving.api.search.application.dto.SearchListingsCommand
import com.coliving.api.search.application.usecase.SearchListingsService
import com.coliving.api.shared.error.InvalidArgumentException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SearchListingsServiceTest {

    private val catalog = FakePublicationCatalogPort()
    private val availability = FakeSearchAvailabilityPort()
    private val service = SearchListingsService(catalog, availability)

    @Test
    fun `returns only published listings`() {
        catalog.add(title = "A")
        catalog.add(title = "B")

        val result = service.search(SearchListingsCommand())

        assertEquals(2, result.size)
    }

    @Test
    fun `filters by city (RF-030)`() {
        val bogota = UUID.randomUUID()
        val medellin = UUID.randomUUID()
        catalog.add(title = "Bogota", cityId = bogota)
        catalog.add(title = "Medellin", cityId = medellin)

        val result = service.search(SearchListingsCommand(cityId = bogota))

        assertEquals(1, result.size)
        assertEquals("Bogota", result.single().title)
    }

    @Test
    fun `filters by budget and currency (RF-030)`() {
        catalog.add(title = "Barata", price = BigDecimal("50000"), currency = "COP")
        catalog.add(title = "Cara", price = BigDecimal("200000"), currency = "COP")
        catalog.add(title = "Dolares", price = BigDecimal("100"), currency = "USD")
        catalog.add(title = "Sin precio", price = null)

        val result = service.search(
            SearchListingsCommand(minPrice = BigDecimal("40000"), maxPrice = BigDecimal("150000"), currency = "COP"),
        )

        assertEquals(listOf("Barata"), result.map { it.title })
    }

    @Test
    fun `filters by date availability (RF-030)`() {
        val free = catalog.add(title = "Libre")
        val occupied = catalog.add(title = "Ocupada")
        availability.unavailable.add(occupied.unitId)

        val result = service.search(
            SearchListingsCommand(availableFrom = LocalDate.now().plusDays(5), availableTo = LocalDate.now().plusDays(8)),
        )

        assertEquals(listOf("Libre"), result.map { it.title })
        assertTrue { free.publicationId != occupied.publicationId }
    }

    @Test
    fun `price ordering is deterministic including ties (RF-032)`() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        catalog.add(publicationId = id1, title = "Empate1", price = BigDecimal("80000"))
        catalog.add(publicationId = id2, title = "Empate2", price = BigDecimal("80000"))
        catalog.add(title = "Barata", price = BigDecimal("50000"))

        val asc = service.search(SearchListingsCommand(sort = ListingSort.PRICE_ASC))
        val ascAgain = service.search(SearchListingsCommand(sort = ListingSort.PRICE_ASC))
        val desc = service.search(SearchListingsCommand(sort = ListingSort.PRICE_DESC))

        assertEquals(asc.map { it.publicationId }, ascAgain.map { it.publicationId })
        assertEquals(3, asc.size)
        assertEquals("Barata", asc.first().title)
        // Ties resolve by publicationId ascending, deterministically, in both sort directions.
        val tieOrder = if (id1 < id2) listOf("Empate1", "Empate2") else listOf("Empate2", "Empate1")
        assertEquals(tieOrder, asc.drop(1).map { it.title })
        assertEquals(tieOrder + listOf("Barata"), desc.map { it.title })
    }

    @Test
    fun `recency ordering is deterministic (RF-032)`() {
        catalog.add(title = "Vieja", updatedAt = Instant.parse("2026-08-01T12:00:00Z"))
        catalog.add(title = "Reciente", updatedAt = Instant.parse("2026-09-01T12:00:00Z"))

        val result = service.search(SearchListingsCommand(sort = ListingSort.RECENT))

        assertEquals(listOf("Reciente", "Vieja"), result.map { it.title })
    }

    @Test
    fun `date range validations`() {
        assertFailsWith<InvalidArgumentException> {
            service.search(SearchListingsCommand(availableFrom = LocalDate.now().plusDays(5)))
        }
        assertFailsWith<InvalidArgumentException> {
            service.search(
                SearchListingsCommand(
                    availableFrom = LocalDate.now().plusDays(8),
                    availableTo = LocalDate.now().plusDays(5),
                ),
            )
        }
        assertFailsWith<InvalidArgumentException> {
            service.search(SearchListingsCommand(minPrice = BigDecimal("200"), maxPrice = BigDecimal("100")))
        }
    }
}
