package com.coliving.api.search.application

import com.coliving.api.search.application.dto.ListingSort
import com.coliving.api.search.application.dto.SearchListingsCommand
import com.coliving.api.search.application.usecase.SearchListingsService
import com.coliving.api.shared.error.InvalidArgumentException
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SearchListingsServiceTest {

    private val catalog = FakePublicationCatalogPort()
    private val service = SearchListingsService(catalog)

    @Test
    fun `returns only published rooms`() {
        catalog.add(roomTitle = "A")
        catalog.add(roomTitle = "B")
        assertEquals(2, service.search(SearchListingsCommand()).size)
    }

    @Test
    fun `filters by location text`() {
        catalog.add(roomTitle = "Habitación norte", cityName = "Bogotá")
        catalog.add(roomTitle = "Habitación playa", cityName = "Cartagena")

        val result = service.search(SearchListingsCommand(q = "cartagena"))
        assertEquals(listOf("Habitación playa"), result.map { it.roomTitle })
    }

    @Test
    fun `filters by monthly price and currency`() {
        catalog.add(roomTitle = "Económica", price = BigDecimal("650000"), currency = "COP")
        catalog.add(roomTitle = "Cara", price = BigDecimal("1800000"), currency = "COP")
        catalog.add(roomTitle = "Dólares", price = BigDecimal("100"), currency = "USD")
        catalog.add(roomTitle = "Sin precio", price = null)

        val result = service.search(
            SearchListingsCommand(minPrice = BigDecimal("600000"), maxPrice = BigDecimal("900000"), currency = "COP"),
        )
        assertEquals(listOf("Económica"), result.map { it.roomTitle })
    }

    @Test
    fun `availability filter excludes full rooms by default`() {
        catalog.add(roomTitle = "Libre", available = 2)
        catalog.add(roomTitle = "Ocupada", available = 0)

        val default = service.search(SearchListingsCommand())
        val includingFull = service.search(SearchListingsCommand(availableOnly = false))

        assertEquals(listOf("Libre"), default.map { it.roomTitle })
        assertEquals(2, includingFull.size)
        assertTrue(includingFull.single { it.roomTitle == "Ocupada" }.availabilityLabel == "Ocupada")
    }

    @Test
    fun `filters by capacity and room features`() {
        catalog.add(roomTitle = "Simple", capacity = 1, roomType = "INDIVIDUAL", bathroomType = "PRIVADO", airConditioning = true)
        catalog.add(roomTitle = "Compartida", capacity = 3, roomType = "COMPARTIDA", bathroomType = "COMPARTIDO", fan = true)

        val result = service.search(
            SearchListingsCommand(
                minCapacity = 2,
                roomType = "COMPARTIDA",
                bathroomType = "COMPARTIDO",
                fan = true,
            ),
        )
        assertEquals(listOf("Compartida"), result.map { it.roomTitle })
    }

    @Test
    fun `price ordering is deterministic including ties`() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        catalog.add(publicationId = id1, roomTitle = "Empate1", price = BigDecimal("800000"))
        catalog.add(publicationId = id2, roomTitle = "Empate2", price = BigDecimal("800000"))
        catalog.add(roomTitle = "Barata", price = BigDecimal("500000"))

        val asc = service.search(SearchListingsCommand(sort = ListingSort.PRICE_ASC))
        val ascAgain = service.search(SearchListingsCommand(sort = ListingSort.PRICE_ASC))
        val desc = service.search(SearchListingsCommand(sort = ListingSort.PRICE_DESC))

        assertEquals(asc.map { it.publicationId }, ascAgain.map { it.publicationId })
        assertEquals("Barata", asc.first().roomTitle)
        val tieOrder = if (id1 < id2) listOf("Empate1", "Empate2") else listOf("Empate2", "Empate1")
        assertEquals(tieOrder, asc.drop(1).map { it.roomTitle })
        assertEquals(tieOrder + listOf("Barata"), desc.map { it.roomTitle })
    }

    @Test
    fun `recency ordering is deterministic`() {
        catalog.add(roomTitle = "Vieja", updatedAt = Instant.parse("2026-08-01T12:00:00Z"))
        catalog.add(roomTitle = "Reciente", updatedAt = Instant.parse("2026-09-01T12:00:00Z"))

        val result = service.search(SearchListingsCommand(sort = ListingSort.RECENT))
        assertEquals(listOf("Reciente", "Vieja"), result.map { it.roomTitle })
    }

    @Test
    fun `validates price range and capacity`() {
        assertFailsWith<InvalidArgumentException> {
            service.search(SearchListingsCommand(minPrice = BigDecimal("900"), maxPrice = BigDecimal("100")))
        }
        assertFailsWith<InvalidArgumentException> {
            service.search(SearchListingsCommand(minCapacity = 0))
        }
    }
}
