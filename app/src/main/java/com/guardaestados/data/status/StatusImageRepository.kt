package com.guardaestados.data.status

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.guardaestados.domain.status.StatusImage
import com.guardaestados.domain.status.StatusImageCandidate
import com.guardaestados.domain.status.StatusImageClassifier
import com.guardaestados.domain.status.StatusMediaType

class StatusImageRepository(
    context: Context,
    private val classifier: StatusImageClassifier = StatusImageClassifier()
) {
    private val appContext = context.applicationContext
    private val contentResolver: ContentResolver = appContext.contentResolver

    fun loadImages(folderUriString: String): Result<List<StatusImage>> {
        return runCatching {
            val folderUri = Uri.parse(folderUriString)
            val folder = DocumentFile.fromTreeUri(appContext, folderUri)
                ?: throw IllegalStateException("Unable to open selected folder")

            if (!folder.exists() || !folder.isDirectory) {
                throw IllegalStateException("Selected URI is not an available folder")
            }

            mapDocumentsSafely(folder.listFiles().asSequence()) { document ->
                document.toStatusImage()
            }
                .asSequence()
                .sortedWith(compareByDescending<StatusImage> { it.lastModifiedMillis ?: 0L }.thenBy { it.name })
                .toList()
        }
    }

    private fun DocumentFile.toStatusImage(): StatusImage? {
        val fileName = name.orEmpty()
        val sizeBytes = length().takeIf { it > 0L }
        val mimeType = classifier.resolveMimeType(
            documentMimeType = type,
            resolverMimeType = contentResolver.getType(uri),
            name = fileName
        ) ?: return null
        val candidate = StatusImageCandidate(
            name = fileName,
            mimeType = mimeType,
            isDirectory = isDirectory,
            sizeBytes = sizeBytes
        )
        if (!classifier.isAccepted(candidate)) return null
        val mediaType = StatusMediaType.fromMimeType(mimeType)
        val dimensions = if (mediaType == StatusMediaType.Image) contentResolver.readImageDimensions(uri) else null
        val durationMillis = if (mediaType == StatusMediaType.Video) readVideoDurationMillis(uri) else null
        return StatusImage(
            uri = uri,
            name = fileName,
            mimeType = mimeType,
            lastModifiedMillis = lastModified().takeIf { it > 0L },
            sizeBytes = sizeBytes,
            widthPixels = dimensions?.widthPixels,
            heightPixels = dimensions?.heightPixels,
            durationMillis = durationMillis,
            mediaType = mediaType
        )
    }

    private fun ContentResolver.readImageDimensions(uri: Uri): ImageDimensions? {
        return runCatching {
            openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, options)
                ImageDimensions(
                    widthPixels = options.outWidth.takeIf { it > 0 },
                    heightPixels = options.outHeight.takeIf { it > 0 }
                ).takeIf { dimensions -> dimensions.widthPixels != null && dimensions.heightPixels != null }
            }
        }.getOrNull()
    }

    private fun readVideoDurationMillis(uri: Uri): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).positiveLongOrNull()
        } catch (exception: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun String?.positiveLongOrNull(): Long? {
        return this?.toLongOrNull()?.takeIf { it > 0L }
    }

    private data class ImageDimensions(
        val widthPixels: Int?,
        val heightPixels: Int?
    )
}

internal fun <T, R : Any> mapDocumentsSafely(
    documents: Sequence<T>,
    transform: (T) -> R?
): List<R> {
    return documents.mapNotNull { document ->
        runCatching { transform(document) }.getOrNull()
    }.toList()
}
