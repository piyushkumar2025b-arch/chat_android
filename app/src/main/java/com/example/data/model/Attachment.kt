package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AttachmentInfo(
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localUri: String,
    val isImage: Boolean,
    val previewSnippet: String? = null
)
