package com.guardaestados.domain.video

import kotlin.math.abs
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

class SafeVideoTrimNameGenerator {
    fun generate(baseName: String?, range: VideoTrimRange, timestampMillis: Long): String {
        val normalizedBase = baseName
            ?.substringBeforeLast('.')
            ?.replace(UNSAFE_CHARS, "_")
            ?.trim('_')
            ?.take(MAX_BASE_LENGTH)
            ?.ifBlank { null }
            ?: DEFAULT_BASE
        val start = range.startSeconds.toString().padStart(4, '0')
        val end = range.endSeconds.toString().padStart(4, '0')
        return "${normalizedBase}_recorte_${start}_${end}_$timestampMillis.mp4"
    }

    private companion object {
        const val DEFAULT_BASE = "video"
        const val MAX_BASE_LENGTH = 32
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

enum class VideoTrimRangeValidation {
    Valid,
    UnknownDuration,
    StartNotBeforeEnd,
    TooShort,
    OutsideDuration
}

class VideoTrimPlanner {
    fun defaultRange(durationMs: Long?): VideoTrimRange {
        val durationSeconds = durationSeconds(durationMs)
        if (durationSeconds < MIN_TRIM_SECONDS) {
            return VideoTrimRange(startSeconds = 0, endSeconds = 0)
        }
        return VideoTrimRange(startSeconds = 0, endSeconds = durationSeconds)
    }

    fun durationSeconds(range: VideoTrimRange): Int {
        return range.durationSeconds.coerceAtLeast(0)
    }

    fun validate(range: VideoTrimRange, durationMs: Long?): VideoTrimRangeValidation {
        val totalSeconds = durationSeconds(durationMs)
        return when {
            totalSeconds <= 0 -> VideoTrimRangeValidation.UnknownDuration
            range.startSeconds < 0 || range.endSeconds > totalSeconds -> VideoTrimRangeValidation.OutsideDuration
            range.startSeconds >= range.endSeconds -> VideoTrimRangeValidation.StartNotBeforeEnd
            range.durationSeconds < MIN_TRIM_SECONDS -> VideoTrimRangeValidation.TooShort
            else -> VideoTrimRangeValidation.Valid
        }
    }

    fun formatSeconds(seconds: Int): String {
        val safeSeconds = seconds.coerceAtLeast(0)
        val minutes = safeSeconds / SECONDS_PER_MINUTE
        val remainingSeconds = safeSeconds % SECONDS_PER_MINUTE
        return "%d:%02d".format(minutes, remainingSeconds)
    }

    fun durationSeconds(durationMs: Long?): Int {
        return durationMs?.takeIf { it > 0L }?.div(MILLIS_PER_SECOND)?.toInt() ?: 0
    }

    fun coerceRange(
        startSeconds: Int,
        endSeconds: Int,
        durationMs: Long?,
        previousRange: VideoTrimRange? = null
    ): VideoTrimRange {
        val totalSeconds = durationSeconds(durationMs)
        if (totalSeconds < MIN_TRIM_SECONDS) {
            return VideoTrimRange(startSeconds = 0, endSeconds = 0)
        }

        val safeStart = startSeconds.coerceIn(0, totalSeconds - MIN_TRIM_SECONDS)
        val safeEnd = endSeconds.coerceIn(MIN_TRIM_SECONDS, totalSeconds)
        if (safeStart < safeEnd) {
            return VideoTrimRange(startSeconds = safeStart, endSeconds = safeEnd)
        }

        val previous = previousRange
        return when {
            previous != null && abs(startSeconds - previous.startSeconds) >= abs(endSeconds - previous.endSeconds) -> {
                val end = safeEnd
                VideoTrimRange(startSeconds = (end - MIN_TRIM_SECONDS).coerceAtLeast(0), endSeconds = end)
            }
            else -> {
                val start = safeStart
                VideoTrimRange(startSeconds = start, endSeconds = (start + MIN_TRIM_SECONDS).coerceAtMost(totalSeconds))
            }
        }
    }

    fun adjustStart(range: VideoTrimRange, deltaSeconds: Int, durationMs: Long?): VideoTrimRange {
        val totalSeconds = durationSeconds(durationMs)
        if (totalSeconds < MIN_TRIM_SECONDS) {
            return VideoTrimRange(startSeconds = 0, endSeconds = 0)
        }
        val maxStart = (range.endSeconds - MIN_TRIM_SECONDS).coerceIn(0, totalSeconds - MIN_TRIM_SECONDS)
        return range.copy(startSeconds = (range.startSeconds + deltaSeconds).coerceIn(0, maxStart))
    }

    fun adjustEnd(range: VideoTrimRange, deltaSeconds: Int, durationMs: Long?): VideoTrimRange {
        val totalSeconds = durationSeconds(durationMs)
        if (totalSeconds < MIN_TRIM_SECONDS) {
            return VideoTrimRange(startSeconds = 0, endSeconds = 0)
        }
        val minEnd = (range.startSeconds + MIN_TRIM_SECONDS).coerceIn(MIN_TRIM_SECONDS, totalSeconds)
        return range.copy(endSeconds = (range.endSeconds + deltaSeconds).coerceIn(minEnd, totalSeconds))
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
        const val SECONDS_PER_MINUTE = 60
        const val MIN_TRIM_SECONDS = 1
    }
}
