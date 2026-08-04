package com.guardaestados.data.folder

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.folderSelectionDataStore by preferencesDataStore(name = "folder_selection")

class FolderSelectionRepository(context: Context) {
    private val appContext = context.applicationContext

    val selectionState: Flow<FolderSelectionState> = appContext.folderSelectionDataStore.data
        .map { preferences ->
            val uriString = preferences[SELECTED_FOLDER_URI]
            when {
                uriString.isNullOrBlank() -> FolderSelectionState.NotSelected
                appContext.hasPersistedReadWritePermission(Uri.parse(uriString)) -> {
                    FolderSelectionState.Selected(uriString)
                }
                else -> FolderSelectionState.PermissionLost(uriString)
            }
        }
        .catch { emit(FolderSelectionState.NotSelected) }

    suspend fun saveSelectedFolder(uri: Uri) {
        appContext.folderSelectionDataStore.edit { preferences ->
            preferences[SELECTED_FOLDER_URI] = uri.toString()
        }
    }

    suspend fun forgetSelectedFolder() {
        var selectedUri: Uri? = null
        appContext.folderSelectionDataStore.edit { preferences ->
            preferences[SELECTED_FOLDER_URI]?.let { uriString ->
                selectedUri = Uri.parse(uriString)
            }
            preferences.remove(SELECTED_FOLDER_URI)
        }
        selectedUri?.let(appContext::releasePersistedFolderPermission)
    }

    private fun Context.hasPersistedReadWritePermission(uri: Uri): Boolean {
        return contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission && permission.isWritePermission
        }
    }

    companion object {
        private val SELECTED_FOLDER_URI = stringPreferencesKey("selected_folder_uri")
    }
}

sealed interface FolderSelectionState {
    data object Loading : FolderSelectionState
    data object NotSelected : FolderSelectionState
    data class Selected(val uriString: String) : FolderSelectionState
    data class PermissionLost(val uriString: String) : FolderSelectionState
}

fun Context.takeSelectedFolderPermission(uri: Uri) {
    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    contentResolver.takePersistableUriPermission(uri, flags)
    releaseOtherPersistedTreePermissions(uri)
}

private fun Context.releaseOtherPersistedTreePermissions(selectedUri: Uri) {
    contentResolver.persistedUriPermissions
        .filterNot { permission -> permission.uri == selectedUri }
        .forEach { permission ->
            val flags = permission.releaseFlags()
            if (flags != 0) {
                contentResolver.releasePersistableUriPermission(permission.uri, flags)
            }
        }
}

private fun Context.releasePersistedFolderPermission(uri: Uri) {
    val permission = contentResolver.persistedUriPermissions.firstOrNull { permission ->
        permission.uri == uri
    } ?: return
    val flags = permission.releaseFlags()
    if (flags == 0) return
    runCatching {
        contentResolver.releasePersistableUriPermission(uri, flags)
    }
}

private fun android.content.UriPermission.releaseFlags(): Int {
    var flags = 0
    if (isReadPermission) {
        flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    if (isWritePermission) {
        flags = flags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
    return flags
}