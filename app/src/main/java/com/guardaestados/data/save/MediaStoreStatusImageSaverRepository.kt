package com.guardaestados.data.save

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.guardaestados.data.settings.AppSettingsRepository
import com.guardaestados.data.settings.SaveDestinationState
import com.guardaestados.data.uri.PersistedUriPermissionChecker
import com.guardaestados.domain.save.ImportSavedMediaResult
import com.guardaestados.domain.save.ImportedMediaSaverRepository
import com.guardaestados.domain.save.SafeImageFileNameGenerator
import com.guardaestados.domain.save.SafeVideoFileNameGenerator
import com.guardaestados.domain.save.SaveStatusImageResult
import com.guardaestados.domain.save.StatusImageSaverRepository
import com.guardaestados.domain.save.UniqueFileNameGenerator
import com.guardaestados.domain.status.StatusImage
import com.guardaestados.domain.status.StatusImageCandidate
import com.guardaestados.domain.status.StatusImageClassifier
import com.guardaestados.domain.status.StatusMediaType
import java.io.FileNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreStatusImageSaverRepository(
    context: Context,
    private val settingsRepository: AppSettingsRepository = AppSettingsRepository(context),
    private val permissionChecker: PersistedUriPermissionChecker = PersistedUriPermissionChecker(context),
    private val imageFileNameGenerator: SafeImageFileNameGenerator = SafeImageFileNameGenerator(),
    private val videoFileNameGenerator: SafeVideoFileNameGenerator = SafeVideoFileNameGenerator(),
    private val uniqueFileNameGenerator: UniqueFileNameGenerator = UniqueFileNameGenerator(),
    private val classifier: StatusImageClassifier = StatusImageClassifier(),
    private val clock: () -> Long = System::currentTimeMillis
) : StatusImageSaverRepository, ImportedMediaSaverRepository {
    private val appContext = context.applicationContext
    private val contentResolver: ContentResolver = appContext.contentResolver

    override suspend fun save(image: StatusImage): SaveStatusImageResult = withContext(Dispatchers.IO) {
        try {
            if (!permissionChecker.hasPersistedReadPermissionFor(image.uri)) {
                Log.w(TAG, "Save rejected: source URI is outside persisted tree permissions")
                return@withContext SaveStatusImageResult.Error
            }

            val sourceType = contentResolver.getType(image.uri) ?: image.mimeType
            val saveTarget = image.saveTarget(sourceType)
            when (val destinationState = settingsRepository.currentSaveDestinationState()) {
                SaveDestinationState.Default -> saveToMediaStore(image.uri, sourceType, saveTarget)
                is SaveDestinationState.Custom -> saveToCustomDestination(image.uri, sourceType, saveTarget, destinationState)
                is SaveDestinationState.PermissionLost -> SaveStatusImageResult.DestinationPermissionLost
                is SaveDestinationState.Unavailable -> SaveStatusImageResult.DestinationUnavailable
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Save failed while preparing media copy", exception)
            SaveStatusImageResult.Error
        }
    }

    override suspend fun importMedia(uri: Uri): ImportSavedMediaResult = withContext(Dispatchers.IO) {
        try {
            if (!uri.isReadable()) {
                Log.w(TAG, "Import rejected: selected media could not be opened")
                return@withContext ImportSavedMediaResult.Missing
            }
            val source = uri.toImportSource()
                ?: return@withContext ImportSavedMediaResult.Unsupported

            val saveTarget = source.saveTarget()
            val result = when (val destinationState = settingsRepository.currentSaveDestinationState()) {
                SaveDestinationState.Default -> saveToMediaStore(
                    sourceUri = source.uri,
                    sourceType = source.mimeType,
                    saveTarget = saveTarget,
                    ensureUniqueName = true
                )
                is SaveDestinationState.Custom -> saveToCustomDestination(source.uri, source.mimeType, saveTarget, destinationState)
                is SaveDestinationState.PermissionLost -> SaveStatusImageResult.DestinationPermissionLost
                is SaveDestinationState.Unavailable -> SaveStatusImageResult.DestinationUnavailable
            }
            result.toImportResult()
        } catch (exception: FileNotFoundException) {
            Log.w(TAG, "Import rejected: selected media no longer exists", exception)
            ImportSavedMediaResult.Missing
        } catch (exception: SecurityException) {
            Log.e(TAG, "Import failed because selected media permission was denied", exception)
            ImportSavedMediaResult.Error
        } catch (exception: Exception) {
            Log.e(TAG, "Import failed while preparing media copy", exception)
            ImportSavedMediaResult.Error
        }
    }

    private fun saveToMediaStore(
        sourceUri: Uri,
        sourceType: String,
        saveTarget: SaveTarget,
        ensureUniqueName: Boolean = false
    ): SaveStatusImageResult {
        var destinationUri: Uri? = null
        try {
            val inputStream = contentResolver.openInputStream(sourceUri)
                ?: return SaveStatusImageResult.Error.also {
                    Log.w(TAG, "Save rejected: source media could not be opened")
                }

            val finalSaveTarget = if (ensureUniqueName) {
                saveTarget.copy(
                    displayName = uniqueFileNameGenerator.generate(saveTarget.displayName) { candidate ->
                        displayNameExists(saveTarget.collectionUri, saveTarget.relativePath, candidate)
                    }
                )
            } else {
                saveTarget
            }

            if (!ensureUniqueName && finalSaveTarget.mediaType == StatusMediaType.Video && isDuplicateVideo(finalSaveTarget.displayName)) {
                inputStream.close()
                return SaveStatusImageResult.Duplicate
            }

            inputStream.use { source ->
                val values = ContentValues().apply {
                    put(finalSaveTarget.displayNameColumn, finalSaveTarget.displayName)
                    put(finalSaveTarget.mimeTypeColumn, sourceType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(finalSaveTarget.relativePathColumn, finalSaveTarget.relativePath)
                        put(finalSaveTarget.isPendingColumn, 1)
                    }
                }

                destinationUri = contentResolver.insert(finalSaveTarget.collectionUri, values)
                    ?: return SaveStatusImageResult.Error.also {
                        Log.w(TAG, "Save rejected: MediaStore insert returned null")
                    }

                val outputStream = contentResolver.openOutputStream(destinationUri!!)
                    ?: return SaveStatusImageResult.Error.also {
                        contentResolver.delete(destinationUri!!, null, null)
                        Log.w(TAG, "Save rejected: destination media could not be opened")
                    }

                outputStream.use { destination ->
                    source.copyTo(destination)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val completedValues = ContentValues().apply {
                        put(finalSaveTarget.isPendingColumn, 0)
                    }
                    contentResolver.update(destinationUri!!, completedValues, null, null)
                }
            }
            return SaveStatusImageResult.Success(finalSaveTarget.displayName)
        } catch (exception: Exception) {
            destinationUri?.let { uri -> contentResolver.delete(uri, null, null) }
            Log.e(TAG, "Save failed while copying media to MediaStore", exception)
            return SaveStatusImageResult.Error
        }
    }

    private fun saveToCustomDestination(
        sourceUri: Uri,
        sourceType: String,
        saveTarget: SaveTarget,
        destinationState: SaveDestinationState.Custom
    ): SaveStatusImageResult {
        val rootFolder = DocumentFile.fromTreeUri(appContext, Uri.parse(destinationState.uriString))
            ?: return SaveStatusImageResult.DestinationUnavailable
        if (!rootFolder.exists() || !rootFolder.isDirectory || !rootFolder.canWrite()) {
            return SaveStatusImageResult.DestinationUnavailable
        }

        val appFolder = rootFolder.getOrCreateDirectory(APP_DESTINATION_FOLDER)
            ?: return SaveStatusImageResult.DestinationUnavailable
        val mediaFolderName = if (saveTarget.mediaType == StatusMediaType.Video) VIDEO_DESTINATION_FOLDER else IMAGE_DESTINATION_FOLDER
        val mediaFolder = appFolder.getOrCreateDirectory(mediaFolderName)
            ?: return SaveStatusImageResult.DestinationUnavailable

        val displayName = uniqueFileNameGenerator.generate(saveTarget.displayName) { candidate ->
            mediaFolder.findFile(candidate) != null
        }
        val destinationFile = mediaFolder.createFile(sourceType, displayName)
            ?: return SaveStatusImageResult.DestinationUnavailable

        return try {
            val inputStream = contentResolver.openInputStream(sourceUri)
                ?: return SaveStatusImageResult.Error.also { destinationFile.delete() }
            val outputStream = contentResolver.openOutputStream(destinationFile.uri)
                ?: return SaveStatusImageResult.DestinationUnavailable.also { destinationFile.delete() }

            inputStream.use { source ->
                outputStream.use { destination ->
                    source.copyTo(destination)
                }
            }
            SaveStatusImageResult.Success(displayName)
        } catch (exception: SecurityException) {
            destinationFile.delete()
            Log.e(TAG, "Save failed because destination permission was lost", exception)
            SaveStatusImageResult.DestinationPermissionLost
        } catch (exception: Exception) {
            destinationFile.delete()
            Log.e(TAG, "Save failed while copying media to custom destination", exception)
            SaveStatusImageResult.DestinationUnavailable
        }
    }

    private fun DocumentFile.getOrCreateDirectory(name: String): DocumentFile? {
        findFile(name)?.let { existing ->
            return if (existing.isDirectory) existing else null
        }
        return createDirectory(name)
    }

    private fun StatusImage.saveTarget(sourceType: String): SaveTarget {
        return saveTarget(
            originalName = name,
            mediaType = mediaType,
            sourceType = sourceType
        )
    }

    private fun ImportSource.saveTarget(): SaveTarget {
        return saveTarget(
            originalName = name,
            mediaType = mediaType,
            sourceType = mimeType
        )
    }

    private fun saveTarget(
        originalName: String,
        mediaType: StatusMediaType,
        sourceType: String
    ): SaveTarget {
        return if (mediaType == StatusMediaType.Video || sourceType.startsWith(VIDEO_MIME_PREFIX)) {
            SaveTarget(
                mediaType = StatusMediaType.Video,
                collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                displayName = videoFileNameGenerator.generate(originalName, sourceType),
                relativePath = VIDEO_SAVE_RELATIVE_PATH,
                displayNameColumn = MediaStore.Video.Media.DISPLAY_NAME,
                mimeTypeColumn = MediaStore.Video.Media.MIME_TYPE,
                relativePathColumn = MediaStore.Video.Media.RELATIVE_PATH,
                isPendingColumn = MediaStore.Video.Media.IS_PENDING
            )
        } else {
            SaveTarget(
                mediaType = StatusMediaType.Image,
                collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                displayName = imageFileNameGenerator.generate(originalName, sourceType, clock()),
                relativePath = IMAGE_SAVE_RELATIVE_PATH,
                displayNameColumn = MediaStore.Images.Media.DISPLAY_NAME,
                mimeTypeColumn = MediaStore.Images.Media.MIME_TYPE,
                relativePathColumn = MediaStore.Images.Media.RELATIVE_PATH,
                isPendingColumn = MediaStore.Images.Media.IS_PENDING
            )
        }
    }

    private fun isDuplicateVideo(displayName: String): Boolean {
        return displayNameExists(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, VIDEO_SAVE_RELATIVE_PATH, displayName)
    }

    private fun displayNameExists(
        collectionUri: Uri,
        relativePath: String,
        displayName: String
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(relativePath, displayName)
        return contentResolver.query(
            collectionUri,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor -> cursor.moveToFirst() } == true
    }

    private fun Uri.isReadable(): Boolean {
        return contentResolver.openInputStream(this)?.use { true } == true
    }

    private fun Uri.toImportSource(): ImportSource? {
        val metadata = queryOpenableMetadata()
        val sourceName = metadata.displayName
            ?: lastPathSegment
            ?: return null
        val mimeType = classifier.resolveMimeType(contentResolver.getType(this), sourceName)
            ?: return null
        val candidate = StatusImageCandidate(
            name = sourceName,
            mimeType = mimeType,
            isDirectory = false,
            sizeBytes = metadata.sizeBytes
        )
        if (!classifier.isAccepted(candidate)) return null
        return ImportSource(
            uri = this,
            name = sourceName,
            mimeType = mimeType,
            mediaType = StatusMediaType.fromMimeType(mimeType)
        )
    }

    private fun Uri.queryOpenableMetadata(): OpenableMetadata {
        return runCatching {
            contentResolver.query(
                this,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.toOpenableMetadata() else OpenableMetadata()
            } ?: OpenableMetadata()
        }.getOrDefault(OpenableMetadata())
    }

    private fun Cursor.toOpenableMetadata(): OpenableMetadata {
        val displayNameColumn = getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeColumn = getColumnIndex(OpenableColumns.SIZE)
        return OpenableMetadata(
            displayName = getStringOrNull(displayNameColumn),
            sizeBytes = getLongOrNull(sizeColumn)
        )
    }

    private fun Cursor.getStringOrNull(columnIndex: Int): String? {
        return if (columnIndex >= 0 && !isNull(columnIndex)) getString(columnIndex)?.takeIf { it.isNotBlank() } else null
    }

    private fun Cursor.getLongOrNull(columnIndex: Int): Long? {
        return if (columnIndex >= 0 && !isNull(columnIndex)) getLong(columnIndex).takeIf { it > 0L } else null
    }

    private fun SaveStatusImageResult.toImportResult(): ImportSavedMediaResult {
        return when (this) {
            is SaveStatusImageResult.Success -> ImportSavedMediaResult.Success(displayName)
            SaveStatusImageResult.DestinationPermissionLost -> ImportSavedMediaResult.DestinationPermissionLost
            SaveStatusImageResult.DestinationUnavailable -> ImportSavedMediaResult.DestinationUnavailable
            SaveStatusImageResult.Duplicate,
            SaveStatusImageResult.Error -> ImportSavedMediaResult.Error
        }
    }

    private data class ImportSource(
        val uri: Uri,
        val name: String,
        val mimeType: String,
        val mediaType: StatusMediaType
    )

    private data class OpenableMetadata(
        val displayName: String? = null,
        val sizeBytes: Long? = null
    )

    private data class SaveTarget(
        val mediaType: StatusMediaType,
        val collectionUri: Uri,
        val displayName: String,
        val relativePath: String,
        val displayNameColumn: String,
        val mimeTypeColumn: String,
        val relativePathColumn: String,
        val isPendingColumn: String
    )

    private companion object {
        const val TAG = "StatusMediaSaver"
        const val IMAGE_SAVE_RELATIVE_PATH = "Pictures/EstadoGo/Im\u00E1genes"
        const val VIDEO_SAVE_RELATIVE_PATH = "Movies/EstadoGo/Videos/"
        const val VIDEO_MIME_PREFIX = "video/"
        const val APP_DESTINATION_FOLDER = "EstadoGo"
        const val IMAGE_DESTINATION_FOLDER = "Im\u00E1genes"
        const val VIDEO_DESTINATION_FOLDER = "Videos"
    }
}
