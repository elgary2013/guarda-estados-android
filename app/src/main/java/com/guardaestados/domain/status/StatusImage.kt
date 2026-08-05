package com.guardaestados.domain.status

import android.net.Uri

data class StatusImage(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val lastModifiedMillis: Long?,
    val sizeBytes: Long?,
    val widthPixels: Int? = null,
    val heightPixels: Int? = null,
    val mediaType: StatusMediaType = StatusMediaType.fromMimeType(mimeType)
)

enum class StatusMediaType {
    Image,
    Video;

    companion object {
        fun fromMimeType(mimeType: String?): StatusMediaType {
            return if (mimeType?.startsWith("video/") == true) Video else Image
        }
    }
}
