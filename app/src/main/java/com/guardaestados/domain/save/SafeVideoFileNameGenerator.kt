package com.guardaestados.domain.save

class SafeVideoFileNameGenerator {
    fun generate(originalName: String?, mimeType: String?): String {
        val normalizedBase = originalName
            ?.substringBeforeLast('.')
            ?.replace(UNSAFE_CHARS, "_")
            ?.trim('_')
            ?.take(MAX_BASE_LENGTH)
            ?.ifBlank { null }
            ?: DEFAULT_BASE
        return "$normalizedBase.${extensionFor(mimeType)}"
    }

    private fun extensionFor(mimeType: String?): String {
        return when (mimeType?.trim()?.lowercase()) {
            "video/3gpp" -> "3gp"
            "video/webm" -> "webm"
            else -> "mp4"
        }
    }

    private companion object {
        const val DEFAULT_BASE = "video"
        const val MAX_BASE_LENGTH = 48
        val UNSAFE_CHARS = Regex("[^A-Za-z0-9_-]+")
    }
}
