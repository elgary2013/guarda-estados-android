package com.guardaestados.data.settings

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsRepository(
    context: Context,
    private val folderNameFormatter: SaveDestinationFolderNameFormatter = SaveDestinationFolderNameFormatter()
) {
    private val appContext = context.applicationContext

    val themePreference: Flow<AppThemePreference> = appContext.appSettingsDataStore.data
        .map { preferences ->
            AppThemePreference.fromStorageKey(preferences[THEME_PREFERENCE])
        }
        .catch { emit(AppThemePreference.System) }

    val saveDestinationState: Flow<SaveDestinationState> = appContext.appSettingsDataStore.data
        .map { preferences ->
            val uriString = preferences[SAVE_DESTINATION_URI]
            when {
                uriString.isNullOrBlank() -> SaveDestinationState.Default
                !appContext.hasPersistedReadWritePermission(Uri.parse(uriString)) -> {
                    SaveDestinationState.PermissionLost(uriString, folderNameFormatter.format(uriString))
                }
                else -> {
                    val uri = Uri.parse(uriString)
                    val folder = DocumentFile.fromTreeUri(appContext, uri)
                    if (folder?.exists() == true && folder.isDirectory) {
                        SaveDestinationState.Custom(uriString, folder.name ?: folderNameFormatter.format(uriString))
                    } else {
                        SaveDestinationState.Unavailable(uriString, folderNameFormatter.format(uriString))
                    }
                }
            }
        }
        .catch { emit(SaveDestinationState.Default) }

    suspend fun currentSaveDestinationState(): SaveDestinationState {
        return saveDestinationState.first()
    }

    suspend fun saveThemePreference(themePreference: AppThemePreference) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[THEME_PREFERENCE] = themePreference.storageKey
        }
    }

    suspend fun saveDestinationFolder(uri: Uri) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[SAVE_DESTINATION_URI] = uri.toString()
        }
    }

    suspend fun resetSaveDestinationFolder() {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences.remove(SAVE_DESTINATION_URI)
        }
    }

    suspend fun resetThemePreference() {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences.remove(THEME_PREFERENCE)
        }
    }

    private fun Context.hasPersistedReadWritePermission(uri: Uri): Boolean {
        return contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission && permission.isWritePermission
        }
    }


    companion object {
        private val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        private val SAVE_DESTINATION_URI = stringPreferencesKey("save_destination_uri")
    }
}

sealed interface SaveDestinationState {
    data object Default : SaveDestinationState
    data class Custom(val uriString: String, val folderName: String) : SaveDestinationState
    data class PermissionLost(val uriString: String, val folderName: String) : SaveDestinationState
    data class Unavailable(val uriString: String, val folderName: String) : SaveDestinationState
}