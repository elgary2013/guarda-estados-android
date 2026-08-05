package com.guardaestados.data.save

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.guardaestados.data.settings.AppSettingsRepository
import com.guardaestados.data.settings.SaveDestinationState
import com.guardaestados.data.uri.PersistedUriPermissionChecker
import com.guardaestados.domain.save.SafeImageFileNameGenerator
import com.guardaestados.domain.save.SafeVideoFileNameGenerator
import com.guardaestados.domain.save.SaveStatusImageResult
import com.guardaestados.domain.save.StatusImageSaverRepository
import com.guardaestados.domain.save.UniqueFileNameGenerator
import com.guardaestados.domain.status.StatusImage
import com.guardaestados.domain.status.StatusMediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreStatusImageSaverRepository(
    context: Context,
    private val settingsRepository: AppSettingsRepository = AppSettingsRepository(context),
    private val permissionChecker: PersistedUriPermissionChecker = PersistedUriPermissionChecker(context),
    private val imageFileNameGenerator: SafeImageFileNameGenerator = SafeImageFileNameGenerator(),
    private val videoFileNameGenerator: SafeVideoFileNameGenerator = SafeVideoFileNameGenerator(),
    private val uniqueFileNameGenerator: UniqueFileNameGenerator = UniqueFileNameGenerator(),
    private val clock: () -> Long = System::currentTimeMillis
) : StatusImageSaverRepository {
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

    private fun saveToMediaStore(
        sourceUri: Uri,
        sourceType: String,
        saveTarget: SaveTarget
    ): SaveStatusImageResult {
        var destinationUri: Uri? = null
        try {
            val inputStream = contentResolver.openInputStream(sourceUri)
                ?: return SaveStatusImageResult.Error.also {
                    Log.w(TAG, "Save rejected: source media could not be opened")
                }

            if (saveTarget.mediaType == StatusMediaType.Video && isDuplicateVideo(saveTarget.displayName)) {
                inputStream.close()
                return SaveStatusImageResult.Duplicate
            }

            inputStream.use { source ->
                val values = ContentValues().apply {
                    put(saveTarget.displayNameColumn, saveTarget.displayName)
                    put(saveTarget.mimeTypeColumn, sourceType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(saveTarget.relativePathColumn, saveTarget.relativePath)
                        put(saveTarget.isPendingColumn, 1)
                    }
                }

                destinationUri = contentResolver.insert(saveTarget.collectionUri, values)
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
                        put(saveTarget.isPendingColumn, 0)
                    }
                    contentResolver.update(destinationUri!!, completedValues, null, null)
                }
            }
            return SaveStatusImageResult.Success(saveTarget.displayName)
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
        return if (mediaType == StatusMediaType.Video || sourceType.startsWith(VIDEO_MIME_PREFIX)) {
            SaveTarget(
                mediaType = StatusMediaType.Video,
                collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                displayName = videoFileNameGenerator.generate(name, sourceType),
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
                displayName = imageFileNameGenerator.generate(name, sourceType, clock()),
                relativePath = IMAGE_SAVE_RELATIVE_PATH,
                displayNameColumn = MediaStore.Images.Media.DISPLAY_NAME,
                mimeTypeColumn = MediaStore.Images.Media.MIME_TYPE,
                relativePathColumn = MediaStore.Images.Media.RELATIVE_PATH,
                isPendingColumn = MediaStore.Images.Media.IS_PENDING
            )
        }
    }

    private fun isDuplicateVideo(displayName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val projection = arrayOf(MediaStore.Video.Media._ID)
        val selection = "${MediaStore.Video.Media.RELATIVE_PATH} = ? AND ${MediaStore.Video.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(VIDEO_SAVE_RELATIVE_PATH, displayName)
        return contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor -> cursor.moveToFirst() } == true
    }

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
        const val IMAGE_SAVE_RELATIVE_PATH = "Pictures/GuardaEstados"
        const val VIDEO_SAVE_RELATIVE_PATH = "Movies/GuardaEstados/"
        const val VIDEO_MIME_PREFIX = "video/"
        const val APP_DESTINATION_FOLDER = "SocialSaverFull"
        const val IMAGE_DESTINATION_FOLDER = "Im\u00E1genes"
        const val VIDEO_DESTINATION_FOLDER = "Videos"
    }
}