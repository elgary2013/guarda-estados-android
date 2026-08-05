package com.guardaestados.data.video

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.guardaestados.R
import com.guardaestados.domain.video.GeneratedVideoPart
import com.guardaestados.domain.video.VideoPartSharerRepository
import com.guardaestados.domain.video.VideoShareResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.ArrayList

class AndroidVideoPartSharerRepository(
    context: Context
) : VideoPartSharerRepository {
    private val appContext = context.applicationContext
    private val contentResolver = appContext.contentResolver

    override suspend fun sharePart(part: GeneratedVideoPart): VideoShareResult = withContext(Dispatchers.IO) {
        if (!canOpen(part.uri)) return@withContext VideoShareResult.FileUnavailable
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = part.mimeType.ifBlank { DEFAULT_VIDEO_MIME_TYPE }
            putExtra(Intent.EXTRA_STREAM, part.uri)
            clipData = ClipData.newUri(contentResolver, part.displayName, part.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        openChooser(sendIntent, R.string.video_share_part_chooser_title)
    }

    override suspend fun shareAll(parts: List<GeneratedVideoPart>): VideoShareResult = withContext(Dispatchers.IO) {
        if (parts.isEmpty()) return@withContext VideoShareResult.FileUnavailable
        val orderedParts = parts.sortedBy { it.index }
        if (orderedParts.any { !canOpen(it.uri) }) return@withContext VideoShareResult.FileUnavailable
        val uris = ArrayList<Uri>(orderedParts.map { it.uri })
        val clipData = ClipData.newUri(contentResolver, orderedParts.first().displayName, orderedParts.first().uri)
        orderedParts.drop(1).forEach { part ->
            clipData.addItem(ClipData.Item(part.uri))
        }
        val sendIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = DEFAULT_VIDEO_MIME_TYPE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            this.clipData = clipData
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        openChooser(sendIntent, R.string.video_share_all_chooser_title)
    }

    private suspend fun openChooser(sendIntent: Intent, titleRes: Int): VideoShareResult {
        return try {
            if (sendIntent.resolveActivity(appContext.packageManager) == null) {
                return VideoShareResult.NoCompatibleApp
            }
            val chooser = Intent.createChooser(sendIntent, appContext.getString(titleRes)).apply {
                clipData = sendIntent.clipData
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            withContext(Dispatchers.Main) {
                appContext.startActivity(chooser)
            }
            VideoShareResult.ChooserOpened
        } catch (exception: ActivityNotFoundException) {
            Log.w(TAG, "No compatible app found for video share", exception)
            VideoShareResult.NoCompatibleApp
        } catch (exception: Exception) {
            Log.e(TAG, "Video share failed", exception)
            VideoShareResult.Error
        }
    }

    private fun canOpen(uri: Uri): Boolean {
        return try {
            contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (exception: Exception) {
            false
        }
    }

    private companion object {
        const val TAG = "VideoPartShare"
        const val DEFAULT_VIDEO_MIME_TYPE = "video/mp4"
    }
}
