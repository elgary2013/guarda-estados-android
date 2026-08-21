package com.guardaestados.domain.media

import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToLong

class MediaDetailsFormatter {
    fun formatSize(sizeBytes: Long?): String? {
        val bytes = sizeBytes?.takeIf { it > 0L } ?: return null
        val value = if (bytes >= BYTES_PER_MB) {
            bytes.toDouble() / BYTES_PER_MB
        } else {
            bytes.toDouble() / BYTES_PER_KB
        }
        val unit = if (bytes >= BYTES_PER_MB) "MB" else "KB"
        return String.format(Locale.US, "%.1f %s", value, unit)
    }

    fun formatDateTime(
        millis: Long?,
        locale: Locale = Locale.getDefault(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String? {
        val value = millis?.takeIf { it > 0L } ?: return null
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale)
            .apply { this.timeZone = timeZone }
            .format(Date(value))
    }

    fun formatDuration(durationMillis: Long?): String? {
        val totalSeconds = durationMillis?.takeIf { it > 0L }?.let { millis ->
            (millis.toDouble() / MILLIS_PER_SECOND).roundToLong()
        } ?: return null
        val hours = totalSeconds / SECONDS_PER_HOUR
        val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        return if (hours > 0) {
            "%d:%02d:%02d".format(Locale.US, hours, minutes, seconds)
        } else {
            "%d:%02d".format(Locale.US, minutes, seconds)
        }
    }

    fun formatMimeType(mimeType: String?): String? {
        val normalized = mimeType?.trim()?.lowercase(Locale.US).orEmpty()
        return when (normalized) {
            "image/jpeg" -> "JPG"
            "image/png" -> "PNG"
            "image/webp" -> "WebP"
            "image/gif" -> "GIF"
            "video/mp4" -> "MP4"
            "video/webm" -> "WebM"
            "video/3gpp",
            "video/3gpp2" -> "3GP"
            "video/x-matroska" -> "MKV"
            else -> mimeType?.trim()?.takeIf { it.isNotBlank() }
        }
    }

    private companion object {
        const val BYTES_PER_KB = 1024.0
        const val BYTES_PER_MB = 1024.0 * 1024.0
        const val MILLIS_PER_SECOND = 1000.0
        const val SECONDS_PER_MINUTE = 60L
        const val SECONDS_PER_HOUR = 60L * SECONDS_PER_MINUTE
    }
}
