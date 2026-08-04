package com.guardaestados.domain.status

import android.net.Uri

data class StatusImage(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val lastModifiedMillis: Long?,
    val sizeBytes: Long?
)
