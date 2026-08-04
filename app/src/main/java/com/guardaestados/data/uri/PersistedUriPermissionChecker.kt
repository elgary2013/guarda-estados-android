package com.guardaestados.data.uri

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

class PersistedUriPermissionChecker(
    context: Context
) {
    private val appContext = context.applicationContext

    fun hasPersistedReadPermissionFor(uri: Uri): Boolean {
        return appContext.contentResolver.persistedUriPermissions.any { permission ->
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
}
