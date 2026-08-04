package com.guardaestados.data.status

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.guardaestados.domain.status.StatusImage
import com.guardaestados.domain.status.StatusImageCandidate
import com.guardaestados.domain.status.StatusImageClassifier

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

            folder.listFiles()
                .asSequence()
                .filter { document -> document.isAcceptedImage() }
                .mapNotNull { document -> document.toStatusImage() }
                .sortedWith(compareByDescending<StatusImage> { it.lastModifiedMillis ?: 0L }.thenBy { it.name })
                .toList()
        }
    }

    private fun DocumentFile.isAcceptedImage(): Boolean {
        val candidate = StatusImageCandidate(
            name = name,
            mimeType = type,
            isDirectory = isDirectory,
            sizeBytes = length().takeIf { it >= 0L }
        )
        return classifier.isAccepted(candidate)
    }

    private fun DocumentFile.toStatusImage(): StatusImage? {
        val normalizedMimeType = classifier.normalizeMimeType(type) ?: return null
        val resolvedType = contentResolver.getType(uri)?.let(classifier::normalizeMimeType)
        return StatusImage(
            uri = uri,
            name = name.orEmpty(),
            mimeType = resolvedType ?: normalizedMimeType,
            lastModifiedMillis = lastModified().takeIf { it > 0L },
            sizeBytes = length().takeIf { it > 0L }
        )
    }
}
