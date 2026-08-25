package com.coliving.api.identity.infrastructure.storage

import com.coliving.api.identity.application.port.out.DocumentStorage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Local-disk implementation of [DocumentStorage] (RF-012: stored outside the
 * public web root). The produced key is opaque; swap this adapter for an
 * S3-compatible store without touching the application layer.
 */
@Component
class LocalDocumentStorage(
    @Value("\${identity.storage.directory:./storage/identity-documents}")
    private val directory: String,
) : DocumentStorage {

    private val root: Path = Paths.get(directory).toAbsolutePath().normalize()

    override fun store(
        userId: UUID,
        originalName: String,
        contentType: String,
        bytes: ByteArray,
    ): String {
        val userDir = root.resolve(userId.toString())
        Files.createDirectories(userDir)
        val storedName = "${UUID.randomUUID()}${extensionOf(originalName)}"
        Files.write(userDir.resolve(storedName), bytes)
        return "${userId}/$storedName"
    }

    override fun delete(storageKey: String) {
        val target = root.resolve(storageKey).normalize()
        if (target.startsWith(root)) {
            Files.deleteIfExists(target)
        }
    }

    private fun extensionOf(fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        return if (dot >= 0) fileName.substring(dot).lowercase() else ""
    }
}