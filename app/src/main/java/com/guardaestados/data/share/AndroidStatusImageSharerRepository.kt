package com.guardaestados.data.share

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.guardaestados.R
import com.guardaestados.data.uri.PersistedUriPermissionChecker
import com.guardaestados.domain.share.ShareImageMimeTypeResolver
import com.guardaestados.domain.share.ShareStatusImageResult
import com.guardaestados.domain.share.StatusImageSharerRepository
import com.guardaestados.domain.status.StatusImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidStatusImageSharerRepository(
    context: Context,
    private val permissionChecker: PersistedUriPermissionChecker = PersistedUriPermissionChecker(context),
    private val mimeTypeResolver: ShareImageMimeTypeResolver = ShareImageMimeTypeResolver()
) : StatusImageSharerRepository {
    private val appContext = context.applicationContext
    private val contentResolver: ContentResolver = appContext.contentResolver

    override suspend fun share(image: StatusImage): ShareStatusImageResult = withContext(Dispatchers.IO) {
        try {
            if (!permissionChecker.hasPersistedReadPermissionFor(image.uri)) {
                Log.w(TAG, "Share rejected: source URI is outside persisted tree permissions")
                return@withContext ShareStatusImageResult.Error
            }

            contentResolver.openInputStream(image.uri)?.use {
                // Opening the stream verifies that the document still exists and remains readable.
            } ?: return@withContext ShareStatusImageResult.Error.also {
                Log.w(TAG, "Share rejected: source image could not be opened")
            }

            val mimeType = mimeTypeResolver.resolve(contentResolver.getType(image.uri) ?: image.mimeType)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, image.uri)
                clipData = ClipData.newUri(contentResolver, image.name, image.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (sendIntent.resolveActivity(appContext.packageManager) == null) {
                Log.w(TAG, "Share rejected: no compatible app found")
                return@withContext ShareStatusImageResult.NoCompatibleApp
            }

            val chooser = Intent.createChooser(
                sendIntent,
                appContext.getString(R.string.share_chooser_title)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            withContext(Dispatchers.Main) {
                appContext.startActivity(chooser)
            }
            ShareStatusImageResult.ChooserOpened
        } catch (exception: ActivityNotFoundException) {
            Log.w(TAG, "Share rejected: no compatible app found", exception)
            ShareStatusImageResult.NoCompatibleApp
        } catch (exception: Exception) {
            Log.e(TAG, "Share failed while opening Android chooser", exception)
            ShareStatusImageResult.Error
        }
    }

    private companion object {
        const val TAG = "StatusImageSharer"
    }
}
