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
import com.guardaestados.R
import com.guardaestados.domain.saved.DeleteSavedImageResult
import com.guardaestados.domain.saved.SavedImage
import com.guardaestados.domain.saved.SavedImageDeleteTargetValidator
import com.guardaestados.domain.saved.SavedImagesRepository
import com.guardaestados.domain.saved.ShareSavedImageResult
import com.guardaestados.domain.share.ShareImageMimeTypeResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreSavedImagesRepository(
    context: Context,
    private val deleteTargetValidator: SavedImageDeleteTargetValidator = SavedImageDeleteTargetValidator(),
    private val mimeTypeResolver: ShareImageMimeTypeResolver = ShareImageMimeTypeResolver()
) : SavedImagesRepository {
    private val appContext = context.applicationContext
    private val contentResolver: ContentResolver = appContext.contentResolver

    override fun loadImages(): Result<List<SavedImage>> {
        return runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return@runCatching emptyList()
            }

            loadSavedMedia(
                collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                relativePath = IMAGE_SAVE_RELATIVE_PATH,
                expectedMimePrefix = IMAGE_MIME_PREFIX
            ) + loadSavedMedia(
                collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                relativePath = VIDEO_PARTS_RELATIVE_PATH,
                expectedMimePrefix = VIDEO_MIME_PREFIX
            )
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
                Log.w(TAG, "Delete rejected: URI is not a saved Guarda Estados media item")
                return@withContext DeleteSavedImageResult.InvalidTarget
            }
            SavedImageTargetValidation.Error -> return@withContext DeleteSavedImageResult.Error
        }

        try {
            val deletedRows = contentResolver.delete(uri, null, null)
            if (deletedRows > 0) {
                DeleteSavedImageResult.Deleted
            } else {
                DeleteSavedImageResult.AlreadyMissing
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
                Log.w(TAG, "Share rejected: URI is not a saved Guarda Estados media item")
                return@withContext ShareSavedImageResult.InvalidTarget
            }
            SavedImageTargetValidation.Error -> return@withContext ShareSavedImageResult.Error
        }

        try {
            contentResolver.openInputStream(image.uri)?.use {
                // Opening the stream verifies that the MediaStore item still exists and is readable.
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

    private fun loadSavedMedia(
        collection: Uri,
        relativePath: String,
        expectedMimePrefix: String
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
                            sizeBytes = cursor.getLongOrNull(sizeColumn)
                        )
                    )
                }
            }
        }.orEmpty()
    }

    private fun validateSavedImageTarget(uri: Uri): SavedImageTargetValidation {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return SavedImageTargetValidation.Invalid
        }
        if (uri.scheme != ContentResolver.SCHEME_CONTENT || uri.authority != MEDIA_AUTHORITY) {
            return SavedImageTargetValidation.Invalid
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

    private fun android.database.Cursor.getLongOrNull(columnIndex: Int): Long? {
        return if (isNull(columnIndex)) null else getLong(columnIndex).takeIf { it > 0L }
    }

    private sealed interface SavedImageTargetValidation {
        data class Valid(val mimeType: String) : SavedImageTargetValidation
        data object Missing : SavedImageTargetValidation
        data object Invalid : SavedImageTargetValidation
        data object Error : SavedImageTargetValidation
    }

    private companion object {
        const val TAG = "SavedMedia"
        const val IMAGE_SAVE_RELATIVE_PATH = "Pictures/GuardaEstados/"
        const val VIDEO_PARTS_RELATIVE_PATH = "Movies/GuardaEstados/Partes/"
        const val IMAGE_MIME_PREFIX = "image/"
        const val VIDEO_MIME_PREFIX = "video/"
        const val MEDIA_AUTHORITY = "media"
        const val MILLIS_PER_SECOND = 1000L
    }
}
