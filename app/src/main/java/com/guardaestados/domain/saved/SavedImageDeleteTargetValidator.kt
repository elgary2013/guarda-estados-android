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
        const val IMAGE_SAVE_RELATIVE_PATH = "Pictures/GuardaEstados"
        const val VIDEO_SAVE_RELATIVE_PATH = "Movies/GuardaEstados"
        const val VIDEO_PARTS_RELATIVE_PATH = "Movies/GuardaEstados/Partes"
        const val IMAGE_MIME_PREFIX = "image/"
        const val VIDEO_MIME_PREFIX = "video/"
    }
}
