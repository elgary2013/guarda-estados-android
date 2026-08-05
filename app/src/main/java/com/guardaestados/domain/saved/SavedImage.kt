package com.guardaestados.domain.saved

import android.net.Uri

data class SavedImage(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val dateAddedMillis: Long?,
    val sizeBytes: Long?,
    val mediaType: SavedMediaType = SavedMediaType.fromMimeType(mimeType),
    val origin: SavedMediaOrigin = SavedMediaOrigin.SavedStatus
)

enum class SavedMediaType {
    Image,
    Video;

    companion object {
        fun fromMimeType(mimeType: String?): SavedMediaType {
            return if (mimeType?.startsWith("video/") == true) Video else Image
        }
    }
}

enum class SavedMediaOrigin {
    SavedStatus,
    VideoPart
}
