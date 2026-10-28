package com.coliving.api.accommodation.application.dto

data class UploadPropertyImage(
    val originalFilename: String?,
    val bytes: ByteArray,
)

data class PropertyImageContent(
    val contentType: String,
    val bytes: ByteArray,
)
