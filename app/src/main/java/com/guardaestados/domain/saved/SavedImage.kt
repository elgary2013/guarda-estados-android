package com.guardaestados.domain.saved

import android.net.Uri

data class SavedImage(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val dateAddedMillis: Long?,
    val sizeBytes: Long?
)
