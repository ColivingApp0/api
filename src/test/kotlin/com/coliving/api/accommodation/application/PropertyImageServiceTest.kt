package com.coliving.api.accommodation.application

import com.coliving.api.accommodation.application.dto.UploadPropertyImage
import com.coliving.api.accommodation.application.port.out.AccommodationImageStorage
import com.coliving.api.accommodation.application.usecase.PropertyImageService
import com.coliving.api.accommodation.domain.model.PropertyImage
import com.coliving.api.accommodation.domain.repository.PropertyImageRepository
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.InvalidArgumentException
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.imageio.ImageIO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PropertyImageServiceTest {
    private val hostId = UUID.randomUUID()
    private val propertyId = UUID.randomUUID()
    private val properties = FakePropertyRepository()
    private val images = InMemoryPropertyImageRepository()
    private val storage = InMemoryAccommodationImageStorage()
    private val service = PropertyImageService(properties, images, storage)

    @BeforeEach
    fun setUp() {
        properties.store.clear()
        images.store.clear()
        storage.files.clear()
        properties.save(
            com.coliving.api.accommodation.domain.model.Property.create(
                id = propertyId,
                hostId = hostId,
                title = "Test home",
                description = null,
                cityId = null,
                address = null,
                amenities = emptyList(),
            ),
        )
    }

    @Test
    fun uploadsMultipleImagesAndIdentifiesCover() {
        val saved = service.upload(
            propertyId,
            hostId,
            listOf(
                UploadPropertyImage("living-room.png", pngBytes()),
                UploadPropertyImage("bedroom.png", pngBytes()),
            ),
        )

        assertEquals(2, saved.size)
        assertTrue(saved.first().isCover)
        assertFalse(saved.last().isCover)
        assertEquals(2, storage.files.size)
        assertTrue(saved.all { it.url.startsWith("/api/v1/properties/$propertyId/images/") })
        assertEquals("image/png", saved.first().contentType)
    }

    @Test
    fun rejectsDataThatIsNotAnImage() {
        assertThrows(InvalidArgumentException::class.java) {
            service.upload(
                propertyId,
                hostId,
                listOf(UploadPropertyImage("not-image.png", "not an image".toByteArray())),
            )
        }
        assertTrue(images.store.isEmpty())
        assertTrue(storage.files.isEmpty())
    }

    @Test
    fun rejectsUploadFromNonOwner() {
        assertThrows(ForbiddenException::class.java) {
            service.upload(propertyId, UUID.randomUUID(), listOf(UploadPropertyImage("image.png", pngBytes())))
        }
        assertTrue(images.store.isEmpty())
        assertTrue(storage.files.isEmpty())
    }

    @Test
    fun limitsGalleryToTenImages() {
        val tooMany = (1..11).map { UploadPropertyImage("image-$it.png", pngBytes()) }
        assertThrows(InvalidArgumentException::class.java) {
            service.upload(propertyId, hostId, tooMany)
        }
        assertTrue(images.store.isEmpty())
        assertTrue(storage.files.isEmpty())
    }

    @Test
    fun deletesImageBytesAndMetadata() {
        val saved = service.upload(propertyId, hostId, listOf(UploadPropertyImage("image.png", pngBytes()))).single()
        service.delete(propertyId, saved.id, hostId)
        assertTrue(images.store.isEmpty())
        assertTrue(storage.files.isEmpty())
    }

    private fun pngBytes(): ByteArray {
        val image = BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB)
        val output = ByteArrayOutputStream()
        ImageIO.write(image, "png", output)
        return output.toByteArray()
    }
}

private class InMemoryPropertyImageRepository : PropertyImageRepository {
    val store = linkedMapOf<UUID, PropertyImage>()

    override fun findById(id: UUID): PropertyImage? = store[id]

    override fun findByProperty(propertyId: UUID): List<PropertyImage> =
        store.values.filter { it.propertyId == propertyId }
            .sortedWith(compareBy<PropertyImage> { it.position }.thenBy { it.createdAt }.thenBy { it.id })

    override fun save(image: PropertyImage) {
        store[image.id] = image
    }

    override fun delete(image: PropertyImage) {
        store.remove(image.id)
    }
}

private class InMemoryAccommodationImageStorage : AccommodationImageStorage {
    val files = mutableMapOf<String, ByteArray>()

    override fun store(propertyId: UUID, imageId: UUID, contentType: String, bytes: ByteArray): String {
        val extension = when (contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            else -> error("Unexpected content type")
        }
        val key = "$propertyId/$imageId.$extension"
        files[key] = bytes.copyOf()
        return key
    }

    override fun load(storageKey: String): ByteArray? = files[storageKey]?.copyOf()

    override fun delete(storageKey: String) {
        files.remove(storageKey)
    }
}
