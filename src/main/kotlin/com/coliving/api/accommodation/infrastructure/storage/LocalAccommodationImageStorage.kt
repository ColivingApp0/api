package com.coliving.api.accommodation.infrastructure.storage

import com.coliving.api.accommodation.application.port.out.AccommodationImageStorage
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class LocalAccommodationImageStorage(
    @Value("\${accommodation.images.directory:./storage/accommodation-images}")
    directory: String,
) : AccommodationImageStorage {
    private val root: Path = Paths.get(directory).toAbsolutePath().normalize()

    override fun store(propertyId: UUID, imageId: UUID, contentType: String, bytes: ByteArray): String {
        val extension = when (contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            else -> throw IllegalArgumentException("Unsupported property image content type")
        }
        val propertyDirectory = root.resolve(propertyId.toString()).normalize()
        check(propertyDirectory.startsWith(root)) { "Invalid property image path" }
        Files.createDirectories(propertyDirectory)

        val target = propertyDirectory.resolve("$imageId.$extension").normalize()
        check(target.startsWith(root)) { "Invalid property image path" }
        val temporary = Files.createTempFile(propertyDirectory, ".$imageId-", ".tmp")
        try {
            Files.write(temporary, bytes)
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
        return root.relativize(target).toString().replace(File.separatorChar, '/')
    }

    override fun load(storageKey: String): ByteArray? {
        val target = resolve(storageKey) ?: return null
        if (!Files.isRegularFile(target)) return null
        return Files.readAllBytes(target)
    }

    override fun delete(storageKey: String) {
        val target = resolve(storageKey) ?: return
        Files.deleteIfExists(target)
    }

    private fun resolve(storageKey: String): Path? {
        val candidate = Paths.get(storageKey)
        if (candidate.isAbsolute) return null
        val target = root.resolve(candidate).normalize()
        return target.takeIf { it.startsWith(root) && it != root }
    }
}
