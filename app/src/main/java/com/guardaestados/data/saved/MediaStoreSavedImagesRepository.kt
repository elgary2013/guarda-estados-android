package com.guardaestados.data.saved

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.guardaestados.domain.saved.SavedImage
import com.guardaestados.domain.saved.SavedImagesRepository

class MediaStoreSavedImagesRepository(
    context: Context
) : SavedImagesRepository {
    private val appContext = context.applicationContext
    private val contentResolver = appContext.contentResolver

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

    private fun android.database.Cursor.getLongOrNull(columnIndex: Int): Long? {
        return if (isNull(columnIndex)) null else getLong(columnIndex).takeIf { it > 0L }
    }

    private companion object {
        const val TAG = "SavedImages"
        const val SAVE_RELATIVE_PATH = "Pictures/GuardaEstados/"
        const val MILLIS_PER_SECOND = 1000L
    }
}
