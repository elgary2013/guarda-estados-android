package com.guardaestados.data.save

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import com.guardaestados.domain.save.SafeImageFileNameGenerator
import com.guardaestados.domain.save.SaveStatusImageResult
import com.guardaestados.domain.save.StatusImageSaverRepository
import com.guardaestados.domain.status.StatusImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreStatusImageSaverRepository(
    context: Context,
    private val fileNameGenerator: SafeImageFileNameGenerator = SafeImageFileNameGenerator(),
    private val clock: () -> Long = System::currentTimeMillis
) : StatusImageSaverRepository {
    private val appContext = context.applicationContext
    private val contentResolver: ContentResolver = appContext.contentResolver

    override suspend fun save(image: StatusImage): SaveStatusImageResult = withContext(Dispatchers.IO) {
        val displayName = fileNameGenerator.generate(image.name, image.mimeType, clock())
        var destinationUri: Uri? = null
        try {
            if (!contentResolver.hasPersistedReadPermissionFor(image.uri)) {
                Log.w(TAG, "Save rejected: source URI is outside persisted tree permissions")
                return@withContext SaveStatusImageResult.Error
            }

            val sourceType = contentResolver.getType(image.uri) ?: image.mimeType
            val inputStream = contentResolver.openInputStream(image.uri)
                ?: return@withContext SaveStatusImageResult.Error.also {
                    Log.w(TAG, "Save rejected: source image could not be opened")
                }

            inputStream.use { source ->
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, sourceType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, SAVE_RELATIVE_PATH)
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                destinationUri = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                ) ?: return@withContext SaveStatusImageResult.Error.also {
                    Log.w(TAG, "Save rejected: MediaStore insert returned null")
                }

                val outputStream = contentResolver.openOutputStream(destinationUri!!)
                    ?: return@withContext SaveStatusImageResult.Error.also {
                        contentResolver.delete(destinationUri!!, null, null)
                        Log.w(TAG, "Save rejected: destination image could not be opened")
                    }

                outputStream.use { destination ->
                    source.copyTo(destination)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val completedValues = ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }
                    contentResolver.update(destinationUri!!, completedValues, null, null)
                }
            }
            SaveStatusImageResult.Success(displayName)
        } catch (exception: Exception) {
            destinationUri?.let { uri -> contentResolver.delete(uri, null, null) }
            Log.e(TAG, "Save failed while copying image to MediaStore", exception)
            SaveStatusImageResult.Error
        }
    }

    private fun ContentResolver.hasPersistedReadPermissionFor(uri: Uri): Boolean {
        return persistedUriPermissions.any { permission ->
            permission.isReadPermission && (permission.uri == uri || permission.uri.containsDocument(uri))
        }
    }

    private fun Uri.containsDocument(documentUri: Uri): Boolean {
        return runCatching {
            authority == documentUri.authority &&
                DocumentsContract.isTreeUri(this) &&
                DocumentsContract.isDocumentUri(appContext, documentUri) &&
                DocumentsContract.getDocumentId(documentUri).startsWith(
                    DocumentsContract.getTreeDocumentId(this)
                )
        }.getOrDefault(false)
    }

    private companion object {
        const val TAG = "StatusImageSaver"
        const val SAVE_RELATIVE_PATH = "Pictures/GuardaEstados"
    }
}
