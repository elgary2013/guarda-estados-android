package com.guardaestados.domain.media

data class MediaDetails(
    val type: MediaDetailsType,
    val origin: MediaDetailsOrigin,
    val mimeType: String?,
    val sizeBytes: Long?,
    val dateTimeMillis: Long?,
    val widthPixels: Int?,
    val heightPixels: Int?,
    val durationMillis: Long?
)

enum class MediaDetailsType {
    Image,
    Video
}

enum class MediaDetailsOrigin {
    AuthorizedStatus,
    SavedEstadoGo
}
