package com.coliving.api.search.application

import com.coliving.api.search.application.usecase.FavoriteService
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.NotFoundException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FavoriteServiceTest {

    private val favorites = FakeFavoriteRepository()
    private val catalog = FakePublicationCatalogPort()
    private val service = FavoriteService(favorites, catalog)

    private val userId = UUID.randomUUID()

    private fun publishedPublication(): UUID {
        val publicationId = UUID.randomUUID()
        catalog.add(publicationId = publicationId)
        return publicationId
    }

    @Test
    fun `adds a favorite over a published listing (RF-034)`() {
        val publicationId = publishedPublication()

        val favorite = service.add(userId, publicationId)

        assertEquals(publicationId, favorite.publicationId)
        assertEquals(1, service.listMine(userId).size)
    }

    @Test
    fun `rejects duplicates for the same user`() {
        val publicationId = publishedPublication()
        service.add(userId, publicationId)

        assertFailsWith<ConflictException> { service.add(userId, publicationId) }
        assertEquals(1, service.listMine(userId).size)
    }

    @Test
    fun `rejects favorites over unknown or unpublished listings`() {
        assertFailsWith<NotFoundException> { service.add(userId, UUID.randomUUID()) }
    }

    @Test
    fun `removes an existing favorite and rejects removing unknown ones`() {
        val publicationId = publishedPublication()
        service.add(userId, publicationId)

        service.remove(userId, publicationId)

        assertTrue { service.listMine(userId).isEmpty() }
        assertFailsWith<NotFoundException> { service.remove(userId, publicationId) }
    }

    @Test
    fun `lists are per user`() {
        val other = UUID.randomUUID()
        val publicationId = publishedPublication()
        service.add(userId, publicationId)

        assertTrue { service.listMine(other).isEmpty() }
        assertEquals(1, service.listMine(userId).size)
    }
}
