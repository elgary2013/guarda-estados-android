package com.guardaestados.data.video

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.guardaestados.domain.video.GeneratedVideoPart
import com.guardaestados.domain.video.SafeVideoPartNameGenerator
import com.guardaestados.domain.video.SelectedVideo
import com.guardaestados.domain.video.VideoMetadataResult
import com.guardaestados.domain.video.VideoSplitPlanner
import com.guardaestados.domain.video.VideoSplitProgress
import com.guardaestados.domain.video.VideoSplitResult
import com.guardaestados.domain.video.VideoSplitterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume

class Media3VideoSplitterRepository(
    context: Context,
    private val planner: VideoSplitPlanner = VideoSplitPlanner(),
    private val nameGenerator: SafeVideoPartNameGenerator = SafeVideoPartNameGenerator()
) : VideoSplitterRepository {
    private val appContext = context.applicationContext
    private val contentResolver: ContentResolver = appContext.contentResolver

    override suspend fun loadVideo(uri: Uri): VideoMetadataResult = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri)?.use {
                // Verifies that the selected video is still readable through Android's granted URI.
            } ?: return@withContext VideoMetadataResult.FileUnavailable

            val displayName = queryDisplayName(uri).ifBlank { DEFAULT_VIDEO_NAME }
            val mimeType = contentResolver.getType(uri).orEmpty().ifBlank { DEFAULT_VIDEO_MIME_TYPE }
            val durationMs = readDurationMs(uri)
            if (durationMs == null || durationMs <= 0L) {
                VideoMetadataResult.EmptyOrUnknownDuration
            } else {
                VideoMetadataResult.Success(
                    SelectedVideo(
                        uri = uri,
                        displayName = displayName,
                        mimeType = mimeType,
                        durationMs = durationMs
                    )
                )
            }
        } catch (exception: SecurityException) {
            Log.e(TAG, "Selected video is no longer readable", exception)
            VideoMetadataResult.FileUnavailable
        } catch (exception: Exception) {
            Log.e(TAG, "Video metadata read failed", exception)
            VideoMetadataResult.Error
        }
    }

    @OptIn(UnstableApi::class)
    override suspend fun splitVideo(
        video: SelectedVideo,
        partDurationSeconds: Int,
        onProgress: (VideoSplitProgress) -> Unit
    ): VideoSplitResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return VideoSplitResult.UnsupportedAndroidVersion
        }
        val segments = planner.plan(video.durationMs, partDurationSeconds)
        if (segments.isEmpty()) return VideoSplitResult.EmptyOrUnknownDuration

        return withContext(Dispatchers.IO) {
            val outputParts = mutableListOf<GeneratedVideoPart>()
            val timestamp = System.currentTimeMillis()
            val tempDir = File(appContext.cacheDir, TEMP_DIRECTORY).apply { mkdirs() }
            try {
                segments.forEachIndexed { position, segment ->
                    onProgress(VideoSplitProgress(currentPart = position, totalParts = segments.size))
                    contentResolver.openInputStream(video.uri)?.close()
                        ?: return@withContext VideoSplitResult.FileUnavailable

                    val outputName = nameGenerator.generate(
                        baseName = video.displayName,
                        partIndex = segment.index,
                        timestampMillis = timestamp
                    )
                    val tempFile = File(tempDir, outputName)
                    if (tempFile.exists()) tempFile.delete()

                    val exportResult = exportSegment(video.uri, segment.startMs, segment.endMs, tempFile)
                    when (exportResult) {
                        SegmentExportResult.Completed -> Unit
                        SegmentExportResult.Cancelled -> {
                            tempFile.delete()
                            return@withContext VideoSplitResult.Cancelled
                        }
                        SegmentExportResult.Failed -> {
                            tempFile.delete()
                            return@withContext VideoSplitResult.ExportError
                        }
                    }

                    val savedUri = copyToMediaStore(tempFile, outputName)
                    tempFile.delete()
                    outputParts += GeneratedVideoPart(
                        index = segment.index,
                        uri = savedUri,
                        displayName = outputName,
                        mimeType = DEFAULT_VIDEO_MIME_TYPE,
                        durationMs = segment.durationMs
                    )
                    onProgress(VideoSplitProgress(currentPart = position + 1, totalParts = segments.size))
                }
                VideoSplitResult.Success(outputParts)
            } catch (exception: IOException) {
                Log.e(TAG, "Video split failed because storage was not writable", exception)
                VideoSplitResult.InsufficientStorage
            } catch (exception: SecurityException) {
                Log.e(TAG, "Video split lost access to the selected file", exception)
                VideoSplitResult.FileUnavailable
            } catch (exception: Exception) {
                Log.e(TAG, "Video split failed", exception)
                VideoSplitResult.ExportError
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use ""
            val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (column >= 0 && !cursor.isNull(column)) cursor.getString(column).orEmpty() else ""
        }.orEmpty()
    }

    private fun readDurationMs(uri: Uri): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } finally {
            retriever.release()
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun exportSegment(
        inputUri: Uri,
        startMs: Long,
        endMs: Long,
        outputFile: File
    ): SegmentExportResult = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            lateinit var transformer: Transformer
            val mediaItem = MediaItem.Builder()
                .setUri(inputUri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(endMs)
                        .build()
                )
                .build()
            transformer = Transformer.Builder(appContext)
                .addListener(
                    object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            if (continuation.isActive) continuation.resume(SegmentExportResult.Completed)
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            Log.e(TAG, "Video segment export failed", exportException)
                            if (continuation.isActive) continuation.resume(SegmentExportResult.Failed)
                        }
                    }
                )
                .build()
            continuation.invokeOnCancellation {
                runCatching { transformer.cancel() }
                outputFile.delete()
            }
            transformer.start(mediaItem, outputFile.absolutePath)
        }
    }

    private fun copyToMediaStore(sourceFile: File, displayName: String): Uri {
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, DEFAULT_VIDEO_MIME_TYPE)
            put(MediaStore.Video.Media.RELATIVE_PATH, VIDEO_PARTS_RELATIVE_PATH)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val outputUri = contentResolver.insert(collection, values)
            ?: throw IOException("MediaStore insert returned null")
        try {
            contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream -> inputStream.copyTo(outputStream) }
            } ?: throw IOException("MediaStore output stream returned null")
            val publishedValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            contentResolver.update(outputUri, publishedValues, null, null)
            return outputUri
        } catch (exception: Exception) {
            contentResolver.delete(outputUri, null, null)
            throw exception
        }
    }

    private sealed interface SegmentExportResult {
        data object Completed : SegmentExportResult
        data object Cancelled : SegmentExportResult
        data object Failed : SegmentExportResult
    }

    private companion object {
        const val TAG = "VideoSplitter"
        const val DEFAULT_VIDEO_NAME = "video"
        const val DEFAULT_VIDEO_MIME_TYPE = "video/mp4"
        const val TEMP_DIRECTORY = "video_parts"
        const val VIDEO_PARTS_RELATIVE_PATH = "Movies/EstadoGo/Videos/Partes/"
    }
}
