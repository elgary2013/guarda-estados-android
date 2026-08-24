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
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.guardaestados.data.settings.AppSettingsRepository
import com.guardaestados.data.settings.SaveDestinationState
import com.guardaestados.domain.video.GeneratedVideoPart
import com.guardaestados.domain.video.SafeVideoPartNameGenerator
import com.guardaestados.domain.video.SafeVideoTrimNameGenerator
import com.guardaestados.domain.video.SelectedVideo
import com.guardaestados.domain.video.VideoMetadataResult
import com.guardaestados.domain.video.VideoSplitPlanner
import com.guardaestados.domain.video.VideoSplitProgress
import com.guardaestados.domain.video.VideoSplitResult
import com.guardaestados.domain.video.VideoSplitterRepository
import com.guardaestados.domain.video.VideoTrimPlanner
import com.guardaestados.domain.video.VideoTrimRange
import com.guardaestados.domain.video.VideoTrimRangeValidation
import com.guardaestados.domain.video.VideoTrimResult
import com.guardaestados.domain.save.UniqueFileNameGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume

class Media3VideoSplitterRepository(
    context: Context,
    private val planner: VideoSplitPlanner = VideoSplitPlanner(),
    private val trimPlanner: VideoTrimPlanner = VideoTrimPlanner(),
    private val nameGenerator: SafeVideoPartNameGenerator = SafeVideoPartNameGenerator(),
    private val trimNameGenerator: SafeVideoTrimNameGenerator = SafeVideoTrimNameGenerator(),
    private val uniqueFileNameGenerator: UniqueFileNameGenerator = UniqueFileNameGenerator(),
    private val settingsRepository: AppSettingsRepository = AppSettingsRepository(context)
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

                    val savedUri = copyToMediaStore(tempFile, outputName, VIDEO_PARTS_RELATIVE_PATH)
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
            } catch (exception: CancellationException) {
                Log.i(TAG, "Video split cancelled", exception)
                VideoSplitResult.Cancelled
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

    @OptIn(UnstableApi::class)
    override suspend fun trimVideo(
        video: SelectedVideo,
        range: VideoTrimRange
    ): VideoTrimResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return VideoTrimResult.UnsupportedAndroidVersion
        }
        return when (trimPlanner.validate(range, video.durationMs)) {
            VideoTrimRangeValidation.Valid -> withContext(Dispatchers.IO) {
                val timestamp = System.currentTimeMillis()
                val tempDir = File(appContext.cacheDir, TEMP_DIRECTORY).apply { mkdirs() }
                val outputName = trimNameGenerator.generate(video.displayName, range, timestamp)
                val tempFile = File(tempDir, outputName)
                if (tempFile.exists()) tempFile.delete()

                try {
                    contentResolver.openInputStream(video.uri)?.close()
                        ?: return@withContext VideoTrimResult.FileUnavailable

                    when (exportSegment(video.uri, range.startMs, range.endMs, tempFile)) {
                        SegmentExportResult.Completed -> Unit
                        SegmentExportResult.Cancelled -> {
                            tempFile.delete()
                            return@withContext VideoTrimResult.Cancelled
                        }
                        SegmentExportResult.Failed -> {
                            tempFile.delete()
                            return@withContext VideoTrimResult.ExportError
                        }
                    }

                    when (val savedOutput = copyTrimToConfiguredDestination(tempFile, outputName)) {
                        is SavedVideoOutputResult.Success -> VideoTrimResult.Success(
                            GeneratedVideoPart(
                                index = 1,
                                uri = savedOutput.uri,
                                displayName = savedOutput.displayName,
                                mimeType = DEFAULT_VIDEO_MIME_TYPE,
                                durationMs = range.durationMs
                            )
                        )
                        SavedVideoOutputResult.DestinationPermissionLost -> VideoTrimResult.DestinationPermissionLost
                        SavedVideoOutputResult.DestinationUnavailable -> VideoTrimResult.DestinationUnavailable
                    }.also {
                        tempFile.delete()
                    }
                } catch (exception: CancellationException) {
                    tempFile.delete()
                    Log.i(TAG, "Video trim cancelled", exception)
                    VideoTrimResult.Cancelled
                } catch (exception: IOException) {
                    tempFile.delete()
                    Log.e(TAG, "Video trim failed because storage was not writable", exception)
                    VideoTrimResult.InsufficientStorage
                } catch (exception: SecurityException) {
                    tempFile.delete()
                    Log.e(TAG, "Video trim lost access to the selected file", exception)
                    VideoTrimResult.FileUnavailable
                } catch (exception: Exception) {
                    tempFile.delete()
                    Log.e(TAG, "Video trim failed", exception)
                    VideoTrimResult.ExportError
                }
            }
            VideoTrimRangeValidation.UnknownDuration -> VideoTrimResult.EmptyOrUnknownDuration
            VideoTrimRangeValidation.StartNotBeforeEnd,
            VideoTrimRangeValidation.TooShort,
            VideoTrimRangeValidation.OutsideDuration -> VideoTrimResult.InvalidRange
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

    private fun copyToMediaStore(
        sourceFile: File,
        displayName: String,
        relativePath: String
    ): Uri {
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, DEFAULT_VIDEO_MIME_TYPE)
            put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
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

    private suspend fun copyTrimToConfiguredDestination(
        sourceFile: File,
        displayName: String
    ): SavedVideoOutputResult {
        return when (val destinationState = settingsRepository.currentSaveDestinationState()) {
            SaveDestinationState.Default -> {
                val uniqueName = uniqueFileNameGenerator.generate(displayName) { candidate ->
                    displayNameExists(VIDEO_SAVE_RELATIVE_PATH, candidate)
                }
                SavedVideoOutputResult.Success(
                    uri = copyToMediaStore(sourceFile, uniqueName, VIDEO_SAVE_RELATIVE_PATH),
                    displayName = uniqueName
                )
            }
            is SaveDestinationState.Custom -> copyTrimToCustomDestination(sourceFile, displayName, destinationState)
            is SaveDestinationState.PermissionLost -> SavedVideoOutputResult.DestinationPermissionLost
            is SaveDestinationState.Unavailable -> SavedVideoOutputResult.DestinationUnavailable
        }
    }

    private fun copyTrimToCustomDestination(
        sourceFile: File,
        displayName: String,
        destinationState: SaveDestinationState.Custom
    ): SavedVideoOutputResult {
        val rootFolder = DocumentFile.fromTreeUri(appContext, Uri.parse(destinationState.uriString))
            ?: return SavedVideoOutputResult.DestinationUnavailable
        if (!rootFolder.exists() || !rootFolder.isDirectory || !rootFolder.canWrite()) {
            return SavedVideoOutputResult.DestinationUnavailable
        }

        val appFolder = rootFolder.getOrCreateDirectory(APP_DESTINATION_FOLDER)
            ?: return SavedVideoOutputResult.DestinationUnavailable
        val videoFolder = appFolder.getOrCreateDirectory(VIDEO_DESTINATION_FOLDER)
            ?: return SavedVideoOutputResult.DestinationUnavailable
        val uniqueName = uniqueFileNameGenerator.generate(displayName) { candidate ->
            videoFolder.findFile(candidate) != null
        }
        val destinationFile = videoFolder.createFile(DEFAULT_VIDEO_MIME_TYPE, uniqueName)
            ?: return SavedVideoOutputResult.DestinationUnavailable

        return try {
            contentResolver.openOutputStream(destinationFile.uri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream -> inputStream.copyTo(outputStream) }
            } ?: return SavedVideoOutputResult.DestinationUnavailable.also { destinationFile.delete() }
            SavedVideoOutputResult.Success(uri = destinationFile.uri, displayName = uniqueName)
        } catch (exception: CancellationException) {
            destinationFile.delete()
            throw exception
        } catch (exception: SecurityException) {
            destinationFile.delete()
            Log.e(TAG, "Video trim failed because custom destination permission was lost", exception)
            SavedVideoOutputResult.DestinationPermissionLost
        } catch (exception: Exception) {
            destinationFile.delete()
            Log.e(TAG, "Video trim failed while copying to custom destination", exception)
            SavedVideoOutputResult.DestinationUnavailable
        }
    }

    private fun displayNameExists(relativePath: String, displayName: String): Boolean {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(relativePath, displayName)
        return contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor -> cursor.moveToFirst() } == true
    }

    private fun DocumentFile.getOrCreateDirectory(name: String): DocumentFile? {
        findFile(name)?.let { existing ->
            return if (existing.isDirectory) existing else null
        }
        return createDirectory(name)
    }

    private sealed interface SegmentExportResult {
        data object Completed : SegmentExportResult
        data object Cancelled : SegmentExportResult
        data object Failed : SegmentExportResult
    }

    private sealed interface SavedVideoOutputResult {
        data class Success(val uri: Uri, val displayName: String) : SavedVideoOutputResult
        data object DestinationPermissionLost : SavedVideoOutputResult
        data object DestinationUnavailable : SavedVideoOutputResult
    }

    private companion object {
        const val TAG = "VideoSplitter"
        const val DEFAULT_VIDEO_NAME = "video"
        const val DEFAULT_VIDEO_MIME_TYPE = "video/mp4"
        const val TEMP_DIRECTORY = "video_parts"
        const val VIDEO_PARTS_RELATIVE_PATH = "Movies/EstadoGo/Videos/Partes/"
        const val VIDEO_SAVE_RELATIVE_PATH = "Movies/EstadoGo/Videos/"
        const val APP_DESTINATION_FOLDER = "EstadoGo"
        const val VIDEO_DESTINATION_FOLDER = "Videos"
        const val MILLIS_PER_SECOND = 1000L
    }
}
