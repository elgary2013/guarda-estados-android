package com.guardaestados.domain.saved

class SavedImageDeleteTargetValidator {
    fun isValid(
        relativePath: String?,
        mimeType: String?
    ): Boolean {
        val normalizedPath = relativePath?.trim()?.trimEnd('/')
        return when {
            normalizedPath == IMAGE_SAVE_RELATIVE_PATH && mimeType?.startsWith(IMAGE_MIME_PREFIX) == true -> true
            normalizedPath == VIDEO_SAVE_RELATIVE_PATH && mimeType?.startsWith(VIDEO_MIME_PREFIX) == true -> true
            normalizedPath == VIDEO_PARTS_RELATIVE_PATH && mimeType?.startsWith(VIDEO_MIME_PREFIX) == true -> true
            else -> false
        }
    }

    private companion object {
        const val IMAGE_SAVE_RELATIVE_PATH = "Pictures/EstadoGo/Im\u00E1genes"
        const val VIDEO_SAVE_RELATIVE_PATH = "Movies/EstadoGo/Videos"
        const val VIDEO_PARTS_RELATIVE_PATH = "Movies/EstadoGo/Videos/Partes"
        const val IMAGE_MIME_PREFIX = "image/"
        const val VIDEO_MIME_PREFIX = "video/"
    }
}
