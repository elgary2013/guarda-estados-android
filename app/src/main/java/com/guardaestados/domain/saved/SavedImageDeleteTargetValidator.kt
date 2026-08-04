package com.guardaestados.domain.saved

class SavedImageDeleteTargetValidator {
    fun isValid(
        relativePath: String?,
        mimeType: String?
    ): Boolean {
        val normalizedPath = relativePath?.trim()?.trimEnd('/')
        return normalizedPath == SAVE_RELATIVE_PATH &&
            mimeType?.startsWith(IMAGE_MIME_PREFIX) == true
    }

    private companion object {
        const val SAVE_RELATIVE_PATH = "Pictures/GuardaEstados"
        const val IMAGE_MIME_PREFIX = "image/"
    }
}
