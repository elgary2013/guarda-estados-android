package com.guardaestados.ui.status

import java.util.Locale

class StatusImagePresentationFormatter {
    fun title(name: String, fallbackDate: String?): String {
        return name.trim().ifBlank { fallbackDate.orEmpty().trim() }
    }

    fun sizeValue(sizeBytes: Long?): String? {
        val bytes = sizeBytes?.takeIf { it > 0L } ?: return null
        val value = if (bytes >= BYTES_PER_MB) {
            bytes.toDouble() / BYTES_PER_MB
        } else {
            bytes.toDouble() / BYTES_PER_KB
        }
        val unit = if (bytes >= BYTES_PER_MB) "MB" else "KB"
        return String.format(Locale.US, "%.1f %s", value, unit)
    }

    fun formatValue(mimeType: String): String? {
        val normalized = mimeType.trim().lowercase(Locale.US)
        return when (normalized) {
            "image/jpeg" -> "JPG"
            "image/png" -> "PNG"
            "image/webp" -> "WebP"
            else -> mimeType.trim().takeIf { it.isNotBlank() }
        }
    }

    fun dimensionsValue(widthPixels: Int?, heightPixels: Int?): String? {
        val width = widthPixels?.takeIf { it > 0 } ?: return null
        val height = heightPixels?.takeIf { it > 0 } ?: return null
        return "${width} x ${height} px"
    }

    private companion object {
        const val BYTES_PER_KB = 1024.0
        const val BYTES_PER_MB = 1024.0 * 1024.0
    }
}