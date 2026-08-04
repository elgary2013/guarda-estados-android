package com.guardaestados.domain.save

class SafeImageFileNameGenerator {
    fun generate(
        originalName: String,
        mimeType: String,
        timestampMillis: Long
    ): String {
        val extension = mimeType.toExtension()
        val baseName = originalName
            .substringBeforeLast('.', originalName)
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_', '.', '-')
            .ifBlank { "estado" }
            .take(48)
        return "${baseName}_${timestampMillis}.${extension}"
    }

    private fun String.toExtension(): String {
        return when (lowercase()) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "img"
        }
    }
}
