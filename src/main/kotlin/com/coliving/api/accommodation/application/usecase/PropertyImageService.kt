package com.coliving.api.accommodation.application.usecase

import com.coliving.api.accommodation.application.dto.PropertyImageContent
import com.coliving.api.accommodation.application.dto.PropertyImageView
import com.coliving.api.accommodation.application.dto.UploadPropertyImage
import com.coliving.api.accommodation.application.port.out.AccommodationImageStorage
import com.coliving.api.accommodation.domain.model.PropertyImage
import com.coliving.api.accommodation.domain.repository.PropertyImageRepository
import com.coliving.api.accommodation.domain.repository.PropertyRepository
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.InvalidArgumentException
import com.coliving.api.shared.error.NotFoundException
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.UUID
import javax.imageio.ImageIO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PropertyImageService(
    private val propertyRepository: PropertyRepository,
    private val imageRepository: PropertyImageRepository,
    private val imageStorage: AccommodationImageStorage,
) {
    @Transactional
    fun upload(
        propertyId: UUID,
        hostId: UUID,
        uploads: List<UploadPropertyImage>,
    ): List<PropertyImageView> {
        val property = propertyRepository.findById(propertyId)
            ?: throw NotFoundException("Property not found")
        checkOwner(property.hostId, hostId)
        if (uploads.isEmpty()) throw InvalidArgumentException("At least one image is required")

        val existing = imageRepository.findByProperty(propertyId)
        if (existing.size + uploads.size > MAX_IMAGES_PER_PROPERTY) {
            throw InvalidArgumentException("A property can have at most $MAX_IMAGES_PER_PROPERTY images")
        }
        val validated = uploads.map(::validateUpload)
        val storedKeys = mutableListOf<String>()
        var position = (existing.maxOfOrNull { it.position } ?: -1) + 1

        try {
            validated.forEach { upload ->
                val imageId = UUID.randomUUID()
                val storageKey = imageStorage.store(propertyId, imageId, upload.contentType, upload.bytes)
                storedKeys += storageKey
                imageRepository.save(
                    PropertyImage(
                        id = imageId,
                        propertyId = propertyId,
                        storageKey = storageKey,
                        originalFilename = upload.originalFilename,
                        contentType = upload.contentType,
                        sizeBytes = upload.bytes.size.toLong(),
                        position = position++,
                        createdAt = Instant.now(),
                    ),
                )
            }
        } catch (exception: Exception) {
            storedKeys.asReversed().forEach { key -> runCatching { imageStorage.delete(key) } }
            throw exception
        }

        return listForProperty(propertyId)
    }

    @Transactional
    fun delete(propertyId: UUID, imageId: UUID, hostId: UUID) {
        val property = propertyRepository.findById(propertyId)
            ?: throw NotFoundException("Property not found")
        checkOwner(property.hostId, hostId)
        val image = imageRepository.findById(imageId)
            ?: throw NotFoundException("Property image not found")
        if (image.propertyId != propertyId) throw NotFoundException("Property image not found")

        imageRepository.delete(image)
        imageStorage.delete(image.storageKey)
    }

    @Transactional(readOnly = true)
    fun listForProperty(propertyId: UUID): List<PropertyImageView> =
        imageRepository.findByProperty(propertyId).mapIndexed { index, image -> image.toView(index == 0) }

    @Transactional(readOnly = true)
    fun content(propertyId: UUID, imageId: UUID): PropertyImageContent {
        val image = imageRepository.findById(imageId)
            ?: throw NotFoundException("Property image not found")
        if (image.propertyId != propertyId) throw NotFoundException("Property image not found")
        val bytes = imageStorage.load(image.storageKey)
            ?: throw NotFoundException("Property image content not found")
        return PropertyImageContent(contentType = image.contentType, bytes = bytes)
    }

    private fun validateUpload(upload: UploadPropertyImage): ValidatedUpload {
        val bytes = upload.bytes
        if (bytes.isEmpty()) throw InvalidArgumentException("Image files cannot be empty")
        if (bytes.size > MAX_IMAGE_SIZE_BYTES) throw InvalidArgumentException("Each image must be 5 MiB or smaller")

        val contentType = detectAndValidateContentType(bytes)
        val filename = upload.originalFilename
            ?.replace('\\', '/')
            ?.substringAfterLast('/')
            ?.trim()
            ?.take(255)
            ?.takeIf { it.isNotBlank() }
            ?: "image"
        return ValidatedUpload(filename, contentType, bytes)
    }

    private fun detectAndValidateContentType(bytes: ByteArray): String {
        val input = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
            ?: throw InvalidArgumentException("File is not a supported image")
        input.use { imageInput ->
            val readers = ImageIO.getImageReaders(imageInput)
            if (!readers.hasNext()) throw InvalidArgumentException("Only JPEG, PNG and GIF images are supported")
            val reader = readers.next()
            try {
                reader.setInput(imageInput, true, true)
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                if (width <= 0 || height <= 0 || width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
                    throw InvalidArgumentException("Image dimensions must not exceed 12000 x 12000 pixels")
                }
                if (width.toLong() * height.toLong() > MAX_IMAGE_PIXELS) {
                    throw InvalidArgumentException("Image must not exceed 40 megapixels")
                }
                return when (reader.formatName.lowercase()) {
                    "jpeg", "jpg" -> "image/jpeg"
                    "png" -> "image/png"
                    "gif" -> "image/gif"
                    else -> throw InvalidArgumentException("Only JPEG, PNG and GIF images are supported")
                }
            } catch (exception: InvalidArgumentException) {
                throw exception
            } catch (exception: Exception) {
                throw InvalidArgumentException("File is not a valid supported image")
            } finally {
                reader.dispose()
            }
        }
    }

    private fun PropertyImage.toView(isCover: Boolean): PropertyImageView =
        PropertyImageView(
            id = id,
            propertyId = propertyId,
            originalFilename = originalFilename,
            contentType = contentType,
            sizeBytes = sizeBytes,
            position = position,
            isCover = isCover,
            url = "/api/v1/properties/$propertyId/images/$id",
        )

    private fun checkOwner(ownerId: UUID, hostId: UUID) {
        if (ownerId != hostId) throw ForbiddenException("Not the owner of this property")
    }

    private data class ValidatedUpload(val originalFilename: String, val contentType: String, val bytes: ByteArray)

    companion object {
        const val MAX_IMAGES_PER_PROPERTY = 10
        private const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024
        private const val MAX_IMAGE_DIMENSION = 12_000
        private const val MAX_IMAGE_PIXELS = 40_000_000L
    }
}
