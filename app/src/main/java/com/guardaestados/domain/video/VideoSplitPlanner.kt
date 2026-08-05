package com.guardaestados.domain.video

import kotlin.math.ceil

data class VideoSegment(
    val index: Int,
    val startMs: Long,
    val endMs: Long
) {
    val durationMs: Long = endMs - startMs
}

class VideoSplitPlanner {
    fun plan(durationMs: Long?, partDurationSeconds: Int): List<VideoSegment> {
        val totalMs = durationMs?.takeIf { it > 0L } ?: return emptyList()
        val partMs = partDurationSeconds.takeIf { it > 0 }?.times(MILLIS_PER_SECOND) ?: return emptyList()
        val count = ceil(totalMs.toDouble() / partMs.toDouble()).toInt()
        return (0 until count).map { position ->
            val start = position * partMs
            VideoSegment(
                index = position + 1,
                startMs = start,
                endMs = minOf(start + partMs, totalMs)
            )
        }.filter { segment -> segment.durationMs > 0L }
    }

    fun estimatedPartCount(durationMs: Long?, partDurationSeconds: Int): Int {
        return plan(durationMs, partDurationSeconds).size
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}

class SafeVideoPartNameGenerator {
    fun generate(baseName: String?, partIndex: Int, timestampMillis: Long): String {
        val normalizedBase = baseName
            ?.substringBeforeLast('.')
            ?.replace(UNSAFE_CHARS, "_")
            ?.trim('_')
            ?.take(MAX_BASE_LENGTH)
            ?.ifBlank { null }
            ?: DEFAULT_BASE
        val safeIndex = partIndex.coerceAtLeast(1).toString().padStart(2, '0')
        return "${normalizedBase}_parte_${safeIndex}_$timestampMillis.mp4"
    }

    private companion object {
        const val DEFAULT_BASE = "video"
        const val MAX_BASE_LENGTH = 36
        val UNSAFE_CHARS = Regex("[^A-Za-z0-9_-]+")
    }
}

class ReadableVideoDurationFormatter {
    fun format(durationMs: Long?): String {
        val totalSeconds = durationMs?.takeIf { it > 0L }?.div(1000L) ?: return UNKNOWN
        val hours = totalSeconds / SECONDS_PER_HOUR
        val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    private companion object {
        const val UNKNOWN = "No disponible"
        const val SECONDS_PER_MINUTE = 60L
        const val SECONDS_PER_HOUR = 3600L
    }
}
