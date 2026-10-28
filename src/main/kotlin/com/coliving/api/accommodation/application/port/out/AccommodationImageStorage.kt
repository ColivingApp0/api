package com.coliving.api.accommodation.application.port.out

import java.util.UUID

interface AccommodationImageStorage {
    fun store(propertyId: UUID, imageId: UUID, contentType: String, bytes: ByteArray): String
    fun load(storageKey: String): ByteArray?
    fun delete(storageKey: String)
}
