package com.guardaestados.domain.video

import android.net.Uri

data class SelectedVideo(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val durationMs: Long?
)

data class GeneratedVideoPart(
    val index: Int,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val durationMs: Long?
)

data class VideoSplitProgress(
    val currentPart: Int,
    val totalParts: Int
) {
    val fraction: Float = if (totalParts <= 0) 0f else currentPart.toFloat() / totalParts.toFloat()
}

data class VideoTrimRange(
    val startSeconds: Int,
    val endSeconds: Int
) {
    val durationSeconds: Int = endSeconds - startSeconds
    val startMs: Long = startSeconds * MILLIS_PER_SECOND
    val endMs: Long = endSeconds * MILLIS_PER_SECOND
    val durationMs: Long = durationSeconds * MILLIS_PER_SECOND

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}

sealed interface VideoMetadataResult {
    data class Success(val video: SelectedVideo) : VideoMetadataResult
    data object EmptyOrUnknownDuration : VideoMetadataResult
    data object FileUnavailable : VideoMetadataResult
    data object Error : VideoMetadataResult
}

sealed interface VideoSplitResult {
    data class Success(val parts: List<GeneratedVideoPart>) : VideoSplitResult
    data object Cancelled : VideoSplitResult
    data object EmptyOrUnknownDuration : VideoSplitResult
    data object FileUnavailable : VideoSplitResult
    data object InsufficientStorage : VideoSplitResult
    data object UnsupportedAndroidVersion : VideoSplitResult
    data object ExportError : VideoSplitResult
}

sealed interface VideoTrimResult {
    data class Success(val trim: GeneratedVideoPart) : VideoTrimResult
    data object Cancelled : VideoTrimResult
    data object InvalidRange : VideoTrimResult
    data object EmptyOrUnknownDuration : VideoTrimResult
    data object FileUnavailable : VideoTrimResult
    data object InsufficientStorage : VideoTrimResult
    data object DestinationPermissionLost : VideoTrimResult
    data object DestinationUnavailable : VideoTrimResult
    data object UnsupportedAndroidVersion : VideoTrimResult
    data object ExportError : VideoTrimResult
}

interface VideoSplitterRepository {
    suspend fun loadVideo(uri: Uri): VideoMetadataResult

    suspend fun splitVideo(
        video: SelectedVideo,
        partDurationSeconds: Int,
        onProgress: (VideoSplitProgress) -> Unit
    ): VideoSplitResult

    suspend fun trimVideo(
        video: SelectedVideo,
        range: VideoTrimRange
    ): VideoTrimResult
}

interface VideoPartSharerRepository {
    suspend fun sharePart(part: GeneratedVideoPart): VideoShareResult
    suspend fun shareAll(parts: List<GeneratedVideoPart>): VideoShareResult
}

sealed interface VideoShareResult {
    data object ChooserOpened : VideoShareResult
    data object NoCompatibleApp : VideoShareResult
    data object FileUnavailable : VideoShareResult
    data object Error : VideoShareResult
}
