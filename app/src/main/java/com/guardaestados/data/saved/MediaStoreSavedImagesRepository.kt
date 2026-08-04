package com.guardaestados.data.saved

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.guardaestados.domain.saved.DeleteSavedImageResult
import com.guardaestados.domain.saved.SavedImage
import com.guardaestados.domain.saved.SavedImageDeleteTargetValidator
import com.guardaestados.domain.saved.SavedImagesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreSavedImagesRepository(
    context: Context,
    private val deleteTargetValidator: SavedImageDeleteTargetValidator = SavedImageDeleteTargetValidator()
) : SavedImagesRepository {
    private val appContext = context.applicationContext
    private val contentResolver: ContentResolver = appContext.contentResolver

    override fun loadImages(): Result<List<SavedImage>> {
        return runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return@runCatching emptyList()
            }

            val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE
            )
            val selection = "${MediaStore.Images.Media.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf(SAVE_RELATIVE_PATH)

            contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                buildList {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val uri = ContentUris.withAppendedId(collection, id)
                        val name = cursor.getString(nameColumn).orEmpty()
                        val mimeType = cursor.getString(mimeColumn).orEmpty()
                        if (!mimeType.startsWith("image/")) continue
                        val dateAddedSeconds = cursor.getLongOrNull(dateAddedColumn)
                        add(
                            SavedImage(
                                uri = uri,
                                name = name,
                                mimeType = mimeType,
                                dateAddedMillis = dateAddedSeconds?.times(MILLIS_PER_SECOND),
                                sizeBytes = cursor.getLongOrNull(sizeColumn)
                            )
                        )
                    }
                }
            }.orEmpty()
        }.onFailure { exception ->
            Log.e(TAG, "Saved images query failed", exception)
        }
    }

    override suspend fun deleteImage(uri: Uri): DeleteSavedImageResult = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return@withContext DeleteSavedImageResult.InvalidTarget
        }

        when (validateSavedImageTarget(uri)) {
            SavedImageTargetValidation.Valid -> Unit
            SavedImageTargetValidation.Missing -> return@withContext DeleteSavedImageResult.AlreadyMissing
            SavedImageTargetValidation.Invalid -> {
                Log.w(TAG, "Delete rejected: URI is not a saved Guarda Estados image")
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
            Log.e(TAG, "Saved image delete failed because permission was denied", exception)
            DeleteSavedImageResult.Error
        } catch (exception: Exception) {
            Log.e(TAG, "Saved image delete failed", exception)
            DeleteSavedImageResult.Error
        }
    }

    private fun validateSavedImageTarget(uri: Uri): SavedImageTargetValidation {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT || uri.authority != MEDIA_AUTHORITY) {
            return SavedImageTargetValidation.Invalid
        }

        val projection = arrayOf(
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.MIME_TYPE
        )
        return try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use SavedImageTargetValidation.Missing
                }
                val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                if (deleteTargetValidator.isValid(
                        relativePath = cursor.getString(relativePathColumn),
                        mimeType = cursor.getString(mimeTypeColumn)
                    )
                ) {
                    SavedImageTargetValidation.Valid
                } else {
                    SavedImageTargetValidation.Invalid
                }
            } ?: SavedImageTargetValidation.Error
        } catch (exception: Exception) {
            Log.e(TAG, "Saved image validation failed", exception)
            SavedImageTargetValidation.Error
        }
    }

    private fun android.database.Cursor.getLongOrNull(columnIndex: Int): Long? {
        return if (isNull(columnIndex)) null else getLong(columnIndex).takeIf { it > 0L }
    }

    private enum class SavedImageTargetValidation {
        Valid,
        Missing,
        Invalid,
        Error
    }

    private companion object {
        const val TAG = "SavedImages"
        const val SAVE_RELATIVE_PATH = "Pictures/GuardaEstados/"
        const val MEDIA_AUTHORITY = "media"
        const val MILLIS_PER_SECOND = 1000L
    }
}
