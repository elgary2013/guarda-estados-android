package com.guardaestados.data.media

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.guardaestados.domain.media.MediaDetails
import com.guardaestados.domain.media.MediaDetailsOrigin
import com.guardaestados.domain.media.MediaDetailsType
import com.guardaestados.domain.saved.SavedImage
import com.guardaestados.domain.saved.SavedMediaType
import com.guardaestados.domain.status.StatusImage
import com.guardaestados.domain.status.StatusMediaType

class AndroidMediaDetailsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val contentResolver: ContentResolver = appContext.contentResolver

    fun loadStatusDetails(image: StatusImage): MediaDetails {
        val mediaType = if (image.mediaType == StatusMediaType.Video) MediaDetailsType.Video else MediaDetailsType.Image
        val basicMetadata = queryBasicMetadata(image.uri)
        val probedMetadata = probeMedia(image.uri, mediaType)
        return MediaDetails(
            type = mediaType,
            origin = MediaDetailsOrigin.AuthorizedStatus,
            mimeType = resolvedMimeType(image.uri, image.mimeType),
            sizeBytes = basicMetadata.sizeBytes ?: image.sizeBytes,
            dateTimeMillis = basicMetadata.dateTimeMillis ?: image.lastModifiedMillis,
            widthPixels = image.widthPixels ?: probedMetadata.widthPixels,
            heightPixels = image.heightPixels ?: probedMetadata.heightPixels,
            durationMillis = probedMetadata.durationMillis
        )
    }

    fun loadSavedDetails(image: SavedImage): MediaDetails {
        val mediaType = if (image.mediaType == SavedMediaType.Video) MediaDetailsType.Video else MediaDetailsType.Image
        val basicMetadata = queryBasicMetadata(image.uri)
        val probedMetadata = probeMedia(image.uri, mediaType)
        return MediaDetails(
            type = mediaType,
            origin = MediaDetailsOrigin.SavedEstadoGo,
            mimeType = resolvedMimeType(image.uri, image.mimeType),
            sizeBytes = basicMetadata.sizeBytes ?: image.sizeBytes,
            dateTimeMillis = basicMetadata.dateTimeMillis ?: image.dateAddedMillis,
            widthPixels = probedMetadata.widthPixels,
            heightPixels = probedMetadata.heightPixels,
            durationMillis = probedMetadata.durationMillis
        )
    }

    private fun resolvedMimeType(uri: Uri, fallback: String?): String? {
        return runCatching { contentResolver.getType(uri) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: fallback?.takeIf { it.isNotBlank() }
    }

    private fun queryBasicMetadata(uri: Uri): BasicMetadata {
        return runCatching {
            val projection = arrayOf(
                OpenableColumns.SIZE,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.DATE_ADDED
            )
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use BasicMetadata()
                BasicMetadata(
                    sizeBytes = cursor.longOrNull(OpenableColumns.SIZE),
                    dateTimeMillis = cursor.longOrNull(MediaStore.MediaColumns.DATE_MODIFIED)
                        ?.times(MILLIS_PER_SECOND)
                        ?: cursor.longOrNull(MediaStore.MediaColumns.DATE_ADDED)?.times(MILLIS_PER_SECOND)
                )
            } ?: BasicMetadata()
        }.getOrElse { exception ->
            Log.w(TAG, "Unable to query basic media details", exception)
            BasicMetadata()
        }
    }

    private fun probeMedia(uri: Uri, mediaType: MediaDetailsType): ProbedMetadata {
        return if (mediaType == MediaDetailsType.Video) {
            probeVideo(uri)
        } else {
            probeImage(uri)
        }
    }

    private fun probeImage(uri: Uri): ProbedMetadata {
        return runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)
                ProbedMetadata(
                    widthPixels = options.outWidth.takeIf { it > 0 },
                    heightPixels = options.outHeight.takeIf { it > 0 }
                )
            } ?: ProbedMetadata()
        }.getOrElse { exception ->
            Log.w(TAG, "Unable to read image dimensions", exception)
            ProbedMetadata()
        }
    }

    private fun probeVideo(uri: Uri): ProbedMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, uri)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).positiveIntOrNull()
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).positiveIntOrNull()
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).positiveLongOrNull()
            ProbedMetadata(
                widthPixels = width,
                heightPixels = height,
                durationMillis = duration
            )
        } catch (exception: Exception) {
            Log.w(TAG, "Unable to read video details", exception)
            ProbedMetadata()
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun Cursor.longOrNull(columnName: String): Long? {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getLong(index).takeIf { it > 0L } else null
    }

    private fun String?.positiveIntOrNull(): Int? = this?.toIntOrNull()?.takeIf { it > 0 }

    private fun String?.positiveLongOrNull(): Long? = this?.toLongOrNull()?.takeIf { it > 0L }

    private data class BasicMetadata(
        val sizeBytes: Long? = null,
        val dateTimeMillis: Long? = null
    )

    private data class ProbedMetadata(
        val widthPixels: Int? = null,
        val heightPixels: Int? = null,
        val durationMillis: Long? = null
    )

    private companion object {
        const val TAG = "MediaDetails"
        const val MILLIS_PER_SECOND = 1000L
    }
}
