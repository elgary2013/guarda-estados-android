package com.guardaestados.domain.share

class ShareImageMimeTypeResolver {
    fun resolve(mimeType: String?): String {
        return mimeType
            ?.takeIf { it.isNotBlank() && (it.startsWith(IMAGE_TYPE_PREFIX) || it.startsWith(VIDEO_TYPE_PREFIX)) }
            ?: FALLBACK_IMAGE_MIME_TYPE
    }

    private companion object {
        const val IMAGE_TYPE_PREFIX = "image/"
        const val VIDEO_TYPE_PREFIX = "video/"
        const val FALLBACK_IMAGE_MIME_TYPE = "image/*"
    }
}
