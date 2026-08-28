package com.coliving.api.identity.application.port.out

import java.util.UUID

/**
 * Out port for persisting verification documents outside the public web root
 * (RF-012: "se almacena fuera del acceso público"). The pilot uses local disk;
 * the contract allows swapping in an S3-compatible object store later.
 */
interface DocumentStorage {
    /** Stores the bytes and returns an opaque [storageKey] for later access. */
    fun store(userId: UUID, originalName: String, contentType: String, bytes: ByteArray): String

    fun delete(storageKey: String)
}