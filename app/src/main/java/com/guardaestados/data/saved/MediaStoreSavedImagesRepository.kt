package com.guardaestados.data.saved

import android.app.RecoverableSecurityException
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.guardaestados.R
import com.guardaestados.data.settings.AppSettingsRepository
import com.guardaestados.data.settings.SaveDestinationState
import com.guardaestados.data.uri.PersistedUriPermissionChecker
import com.guardaestados.domain.saved.DeleteSavedImageResult
import com.guardaestados.domain.saved.OpenSavedImageResult
import com.guardaestados.domain.saved.SavedImage
import com.guardaestados.domain.saved.SavedImageDeleteTargetValidator
import com.guardaestados.domain.saved.SavedImagesRepository
import com.guardaestados.domain.saved.SavedMediaOrigin
import com.guardaestados.domain.saved.ShareSavedImageResult
import com.guardaestados.domain.saved.ShareSavedImagesResult
import com.guardaestados.domain.share.ShareImageMimeTypeResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreSavedImagesRepository(
    context: Context,
    private val deleteTargetValidator: SavedImageDeleteTargetValidator = SavedImageDeleteTargetValidator(),
    private val mimeTypeResolver: ShareImageMimeTypeResolver = ShareImageMimeTypeResolver(),
    private val settingsRepository: AppSettingsRepository = AppSettingsRepository(context),
    private val permissionChecker: PersistedUriPermissionChecker = PersistedUriPermissionChecker(context)
) : SavedImagesRepository {
    private val appContext = context.applicationContext
    private val contentResolver: ContentResolver = appContext.contentResolver

    override suspend fun loadImages(): Result<List<SavedImage>> = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return@runCatching emptyList()
            }

            loadSavedMedia(
                collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                relativePath = IMAGE_SAVE_RELATIVE_PATH,
                expectedMimePrefix = IMAGE_MIME_PREFIX,
                origin = SavedMediaOrigin.SavedStatus
            ) + loadSavedMedia(
                collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                relativePath = VIDEO_SAVE_RELATIVE_PATH,
                expectedMimePrefix = VIDEO_MIME_PREFIX,
                origin = SavedMediaOrigin.SavedStatus
            ) + loadSavedMedia(
                collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                relativePath = VIDEO_PARTS_RELATIVE_PATH,
                expectedMimePrefix = VIDEO_MIME_PREFIX,
                origin = SavedMediaOrigin.VideoPart
            ) + loadCustomDestinationMedia()
        }.onFailure { exception ->
            Log.e(TAG, "Saved media query failed", exception)
        }
    }

    override suspend fun deleteImage(uri: Uri): DeleteSavedImageResult = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return@withContext DeleteSavedImageResult.InvalidTarget
        }

        when (validateSavedImageTarget(uri)) {
            is SavedImageTargetValidation.Valid -> Unit
            SavedImageTargetValidation.Missing -> return@withContext DeleteSavedImageResult.AlreadyMissing
            SavedImageTargetValidation.Invalid -> {
                Log.w(TAG, "Delete rejected: URI is not a saved SocialSaverFull media item")
                return@withContext DeleteSavedImageResult.InvalidTarget
            }
            SavedImageTargetValidation.Error -> return@withContext DeleteSavedImageResult.Error
        }

        try {
            if (uri.authority == MEDIA_AUTHORITY) {
                val deletedRows = contentResolver.delete(uri, null, null)
                if (deletedRows > 0) DeleteSavedImageResult.Deleted else DeleteSavedImageResult.AlreadyMissing
            } else {
                val document = DocumentFile.fromSingleUri(appContext, uri)
                    ?: return@withContext DeleteSavedImageResult.AlreadyMissing
                if (document.delete()) DeleteSavedImageResult.Deleted else DeleteSavedImageResult.Error
            }
        } catch (exception: RecoverableSecurityException) {
            DeleteSavedImageResult.NeedsSystemConfirmation(
                uri = uri,
                intentSender = exception.userAction.actionIntent.intentSender
            )
        } catch (exception: SecurityException) {
            Log.e(TAG, "Saved media delete failed because permission was denied", exception)
            DeleteSavedImageResult.Error
        } catch (exception: Exception) {
            Log.e(TAG, "Saved media delete failed", exception)
            DeleteSavedImageResult.Error
        }
    }

    override suspend fun shareImage(image: SavedImage): ShareSavedImageResult = withContext(Dispatchers.IO) {
        val validation = validateSavedImageTarget(image.uri)
        when (validation) {
            is SavedImageTargetValidation.Valid -> Unit
            SavedImageTargetValidation.Missing -> return@withContext ShareSavedImageResult.AlreadyMissing
            SavedImageTargetValidation.Invalid -> {
                Log.w(TAG, "Share rejected: URI is not a saved SocialSaverFull media item")
                return@withContext ShareSavedImageResult.InvalidTarget
            }
            SavedImageTargetValidation.Error -> return@withContext ShareSavedImageResult.Error
        }

        try {
            contentResolver.openInputStream(image.uri)?.use {
                // Opening the stream verifies that the saved media still exists and is readable.
            } ?: return@withContext ShareSavedImageResult.Error.also {
                Log.w(TAG, "Share rejected: saved media could not be opened")
            }

            val mimeType = mimeTypeResolver.resolve(validation.mimeType)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, image.uri)
                clipData = ClipData.newUri(contentResolver, image.name, image.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (sendIntent.resolveActivity(appContext.packageManager) == null) {
                Log.w(TAG, "Share rejected: no compatible app found")
                return@withContext ShareSavedImageResult.NoCompatibleApp
            }

            val chooser = Intent.createChooser(
                sendIntent,
                appContext.getString(R.string.share_chooser_title)
            ).apply {
                clipData = sendIntent.clipData
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            withContext(Dispatchers.Main) {
                appContext.startActivity(chooser)
            }
            ShareSavedImageResult.ChooserOpened
        } catch (exception: ActivityNotFoundException) {
            Log.w(TAG, "Share rejected: no compatible app found", exception)
            ShareSavedImageResult.NoCompatibleApp
        } catch (exception: SecurityException) {
            Log.e(TAG, "Share failed because saved media read permission was denied", exception)
            ShareSavedImageResult.Error
        } catch (exception: Exception) {
            Log.e(TAG, "Share failed while opening Android chooser", exception)
            ShareSavedImageResult.Error
        }
    }

    override suspend fun shareImages(images: List<SavedImage>): ShareSavedImagesResult = withContext(Dispatchers.IO) {
        if (images.isEmpty()) {
            return@withContext ShareSavedImagesResult.NothingToShare(failedCount = 0)
        }

        val shareableImages = mutableListOf<ShareableSavedImage>()
        var failedCount = 0
        images.forEach { image ->
            val validation = validateSavedImageTarget(image.uri)
            when (validation) {
                is SavedImageTargetValidation.Valid -> {
                    val readable = try {
                        contentResolver.openInputStream(image.uri)?.use {
                            // Verifies that the saved media still exists and is readable before sharing.
                        } != null
                    } catch (exception: Exception) {
                        Log.w(TAG, "Saved media skipped from multi-share because it could not be opened", exception)
                        false
                    }
                    if (readable) {
                        shareableImages += ShareableSavedImage(
                            uri = image.uri,
                            name = image.name,
                            mimeType = validation.mimeType
                        )
                    } else {
                        failedCount++
                    }
                }

                SavedImageTargetValidation.Missing,
                SavedImageTargetValidation.Invalid,
                SavedImageTargetValidation.Error -> failedCount++
            }
        }

        if (shareableImages.isEmpty()) {
            return@withContext ShareSavedImagesResult.NothingToShare(failedCount = failedCount)
        }

        try {
            val uris = ArrayList(shareableImages.map { image -> image.uri })
            val sendIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = shareableImages.combinedMimeType()
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                clipData = shareableImages.toClipData()
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (sendIntent.resolveActivity(appContext.packageManager) == null) {
                Log.w(TAG, "Multi-share rejected: no compatible app found")
                return@withContext ShareSavedImagesResult.NoCompatibleApp
            }

            val chooser = Intent.createChooser(
                sendIntent,
                appContext.getString(R.string.share_chooser_title)
            ).apply {
                clipData = sendIntent.clipData
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            withContext(Dispatchers.Main) {
                appContext.startActivity(chooser)
            }
            ShareSavedImagesResult.ChooserOpened(
                sharedCount = shareableImages.size,
                failedCount = failedCount
            )
        } catch (exception: ActivityNotFoundException) {
            Log.w(TAG, "Multi-share rejected: no compatible app found", exception)
            ShareSavedImagesResult.NoCompatibleApp
        } catch (exception: SecurityException) {
            Log.e(TAG, "Multi-share failed because saved media read permission was denied", exception)
            ShareSavedImagesResult.Error
        } catch (exception: Exception) {
            Log.e(TAG, "Multi-share failed while opening Android chooser", exception)
            ShareSavedImagesResult.Error
        }
    }

    override suspend fun openImage(image: SavedImage): OpenSavedImageResult = withContext(Dispatchers.IO) {
        val validation = validateSavedImageTarget(image.uri)
        when (validation) {
            is SavedImageTargetValidation.Valid -> Unit
            SavedImageTargetValidation.Missing -> return@withContext OpenSavedImageResult.AlreadyMissing
            SavedImageTargetValidation.Invalid -> return@withContext OpenSavedImageResult.InvalidTarget
            SavedImageTargetValidation.Error -> return@withContext OpenSavedImageResult.Error
        }

        try {
            contentResolver.openInputStream(image.uri)?.use {
                // Verifies that the saved media still exists and is readable before handing it to another app.
            } ?: return@withContext OpenSavedImageResult.Error

            val mimeType = mimeTypeResolver.resolve(validation.mimeType)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(image.uri, mimeType)
                clipData = ClipData.newUri(contentResolver, image.name, image.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (viewIntent.resolveActivity(appContext.packageManager) == null) {
                return@withContext OpenSavedImageResult.NoCompatibleApp
            }
            val chooser = Intent.createChooser(
                viewIntent,
                appContext.getString(R.string.saved_open_chooser_title)
            ).apply {
                clipData = viewIntent.clipData
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            withContext(Dispatchers.Main) {
                appContext.startActivity(chooser)
            }
            OpenSavedImageResult.ViewerOpened
        } catch (exception: ActivityNotFoundException) {
            OpenSavedImageResult.NoCompatibleApp
        } catch (exception: Exception) {
            Log.e(TAG, "Open saved media failed", exception)
            OpenSavedImageResult.Error
        }
    }

    private fun loadSavedMedia(
        collection: Uri,
        relativePath: String,
        expectedMimePrefix: String,
        origin: SavedMediaOrigin
    ): List<SavedImage> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.SIZE
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(relativePath)
        return contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            buildList {
                while (cursor.moveToNext()) {
                    val mimeType = cursor.getString(mimeColumn).orEmpty()
                    if (!mimeType.startsWith(expectedMimePrefix)) continue
                    val id = cursor.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(collection, id)
                    val dateAddedSeconds = cursor.getLongOrNull(dateAddedColumn)
                    add(
                        SavedImage(
                            uri = uri,
                            name = cursor.getString(nameColumn).orEmpty(),
                            mimeType = mimeType,
                            dateAddedMillis = dateAddedSeconds?.times(MILLIS_PER_SECOND),
                            sizeBytes = cursor.getLongOrNull(sizeColumn),
                            origin = origin
                        )
                    )
                }
            }
        }.orEmpty()
    }

    private suspend fun loadCustomDestinationMedia(): List<SavedImage> {
        val destinationState = settingsRepository.currentSaveDestinationState() as? SaveDestinationState.Custom
            ?: return emptyList()
        val rootFolder = DocumentFile.fromTreeUri(appContext, Uri.parse(destinationState.uriString))
            ?: return emptyList()
        if (!rootFolder.exists() || !rootFolder.isDirectory || !rootFolder.canRead()) return emptyList()

        val appFolder = rootFolder.findFile(APP_DESTINATION_FOLDER)?.takeIf { it.isDirectory }
            ?: return emptyList()
        return loadCustomSavedMedia(appFolder, IMAGE_DESTINATION_FOLDER, IMAGE_MIME_PREFIX) +
            loadCustomSavedMedia(appFolder, VIDEO_DESTINATION_FOLDER, VIDEO_MIME_PREFIX)
    }

    private fun loadCustomSavedMedia(
        appFolder: DocumentFile,
        folderName: String,
        expectedMimePrefix: String
    ): List<SavedImage> {
        val mediaFolder = appFolder.findFile(folderName)?.takeIf { it.isDirectory }
            ?: return emptyList()
        return mediaFolder.listFiles()
            .asSequence()
            .filter { file -> file.isFile }
            .mapNotNull { file -> file.toSavedImage(expectedMimePrefix) }
            .toList()
    }

    private fun DocumentFile.toSavedImage(expectedMimePrefix: String): SavedImage? {
        val mimeType = type ?: contentResolver.getType(uri) ?: return null
        if (!mimeType.startsWith(expectedMimePrefix)) return null
        return SavedImage(
            uri = uri,
            name = name.orEmpty(),
            mimeType = mimeType,
            dateAddedMillis = lastModified().takeIf { it > 0L },
            sizeBytes = length().takeIf { it > 0L },
            origin = SavedMediaOrigin.SavedStatus
        )
    }

    private suspend fun validateSavedImageTarget(uri: Uri): SavedImageTargetValidation {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return SavedImageTargetValidation.Invalid
        }
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) {
            return SavedImageTargetValidation.Invalid
        }
        if (uri.authority != MEDIA_AUTHORITY) {
            return validateCustomSavedImageTarget(uri)
        }

        val projection = arrayOf(
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.MIME_TYPE
        )
        return try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use SavedImageTargetValidation.Missing
                }
                val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val mimeType = cursor.getString(mimeTypeColumn)
                if (deleteTargetValidator.isValid(
                        relativePath = cursor.getString(relativePathColumn),
                        mimeType = mimeType
                    )
                ) {
                    SavedImageTargetValidation.Valid(mimeType.orEmpty())
                } else {
                    SavedImageTargetValidation.Invalid
                }
            } ?: SavedImageTargetValidation.Error
        } catch (exception: Exception) {
            Log.e(TAG, "Saved media validation failed", exception)
            SavedImageTargetValidation.Error
        }
    }

    private suspend fun validateCustomSavedImageTarget(uri: Uri): SavedImageTargetValidation {
        if (!permissionChecker.hasPersistedReadPermissionFor(uri)) {
            return SavedImageTargetValidation.Invalid
        }
        val savedDocument = findCustomSavedDocument(uri) ?: return SavedImageTargetValidation.Invalid
        return try {
            if (!savedDocument.exists() || !savedDocument.isFile) {
                return SavedImageTargetValidation.Missing
            }
            val mimeType = savedDocument.type ?: contentResolver.getType(uri).orEmpty()
            if (mimeType.startsWith(IMAGE_MIME_PREFIX) || mimeType.startsWith(VIDEO_MIME_PREFIX)) {
                SavedImageTargetValidation.Valid(mimeType)
            } else {
                SavedImageTargetValidation.Invalid
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Custom saved media validation failed", exception)
            SavedImageTargetValidation.Error
        }
    }

    private suspend fun findCustomSavedDocument(uri: Uri): DocumentFile? {
        val destinationState = settingsRepository.currentSaveDestinationState() as? SaveDestinationState.Custom
            ?: return null
        val rootFolder = DocumentFile.fromTreeUri(appContext, Uri.parse(destinationState.uriString))
            ?: return null
        val appFolder = rootFolder.findFile(APP_DESTINATION_FOLDER)?.takeIf { it.isDirectory }
            ?: return null
        return listOf(IMAGE_DESTINATION_FOLDER, VIDEO_DESTINATION_FOLDER)
            .asSequence()
            .mapNotNull { folderName -> appFolder.findFile(folderName)?.takeIf { it.isDirectory } }
            .flatMap { folder -> folder.listFiles().asSequence() }
            .firstOrNull { file -> file.uri == uri }
    }

    private fun android.database.Cursor.getLongOrNull(columnIndex: Int): Long? {
        return if (isNull(columnIndex)) null else getLong(columnIndex).takeIf { it > 0L }
    }

    private sealed interface SavedImageTargetValidation {
        data class Valid(val mimeType: String) : SavedImageTargetValidation
        data object Missing : SavedImageTargetValidation
        data object Invalid : SavedImageTargetValidation
        data object Error : SavedImageTargetValidation
    }

    private data class ShareableSavedImage(
        val uri: Uri,
        val name: String,
        val mimeType: String
    )

    private fun List<ShareableSavedImage>.combinedMimeType(): String {
        return when {
            all { image -> image.mimeType.startsWith(IMAGE_MIME_PREFIX) } -> "$IMAGE_MIME_PREFIX*"
            all { image -> image.mimeType.startsWith(VIDEO_MIME_PREFIX) } -> "$VIDEO_MIME_PREFIX*"
            else -> "*/*"
        }
    }

    private fun List<ShareableSavedImage>.toClipData(): ClipData {
        val firstImage = first()
        return ClipData.newUri(contentResolver, firstImage.name, firstImage.uri).also { clipData ->
            drop(1).forEach { image ->
                clipData.addItem(ClipData.Item(image.uri))
            }
        }
    }

    private companion object {
        const val TAG = "SavedMedia"
        const val IMAGE_SAVE_RELATIVE_PATH = "Pictures/GuardaEstados/"
        const val VIDEO_SAVE_RELATIVE_PATH = "Movies/GuardaEstados/"
        const val VIDEO_PARTS_RELATIVE_PATH = "Movies/GuardaEstados/Partes/"
        const val IMAGE_MIME_PREFIX = "image/"
        const val VIDEO_MIME_PREFIX = "video/"
        const val MEDIA_AUTHORITY = "media"
        const val MILLIS_PER_SECOND = 1000L
        const val APP_DESTINATION_FOLDER = "SocialSaverFull"
        const val IMAGE_DESTINATION_FOLDER = "Im\u00E1genes"
        const val VIDEO_DESTINATION_FOLDER = "Videos"
    }
}
