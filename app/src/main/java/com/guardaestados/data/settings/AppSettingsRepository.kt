package com.guardaestados.data.settings

import android.content.Context
import android.content.Intent
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

    val homeBackgroundUri: Flow<String?> = appContext.appSettingsDataStore.data
        .map { preferences -> preferences[HOME_BACKGROUND_URI] }
        .catch { emit(null) }

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

    suspend fun saveHomeBackground(uri: Uri): HomeBackgroundUpdateResult {
        val previousUriString = currentHomeBackgroundUriString()
        val newUriString = uri.toString()
        val permissionTaken = appContext.takePersistedReadPermission(uri)
        val readable = permissionTaken && appContext.canReadPersistedUri(uri)

        if (!readable) {
            if (permissionTaken && previousUriString != newUriString) {
                appContext.releasePersistedReadPermission(uri)
            }
            return HomeBackgroundUpdateResult.PermissionDenied
        }

        appContext.appSettingsDataStore.edit { preferences ->
            preferences[HOME_BACKGROUND_URI] = newUriString
        }

        previousUriString
            ?.takeIf { uriString -> uriString != newUriString }
            ?.let { uriString -> appContext.releasePersistedReadPermission(Uri.parse(uriString)) }

        return HomeBackgroundUpdateResult.Saved
    }

    suspend fun clearUnreadableHomeBackground(): Boolean {
        val uriString = currentHomeBackgroundUriString() ?: return false
        val uri = Uri.parse(uriString)
        if (appContext.canReadPersistedUri(uri)) return false

        appContext.appSettingsDataStore.edit { preferences ->
            if (preferences[HOME_BACKGROUND_URI] == uriString) {
                preferences.remove(HOME_BACKGROUND_URI)
            }
        }
        appContext.releasePersistedReadPermission(uri)
        return true
    }

    suspend fun clearHomeBackground() {
        var selectedUri: Uri? = null
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[HOME_BACKGROUND_URI]?.let { uriString ->
                selectedUri = Uri.parse(uriString)
            }
            preferences.remove(HOME_BACKGROUND_URI)
        }
        selectedUri?.let { uri -> appContext.releasePersistedReadPermission(uri) }
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

    private suspend fun currentHomeBackgroundUriString(): String? {
        return appContext.appSettingsDataStore.data.first()[HOME_BACKGROUND_URI]
    }

    private fun Context.takePersistedReadPermission(uri: Uri): Boolean {
        return try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            true
        } catch (exception: SecurityException) {
            false
        } catch (exception: IllegalArgumentException) {
            false
        }
    }

    private fun Context.releasePersistedReadPermission(uri: Uri) {
        val permission = contentResolver.persistedUriPermissions.firstOrNull { permission ->
            permission.uri == uri && permission.isReadPermission
        } ?: return
        runCatching {
            contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun Context.canReadPersistedUri(uri: Uri): Boolean {
        val hasPermission = contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission
        }
        if (!hasPermission) return false

        return try {
            contentResolver.openInputStream(uri)?.use { true } == true
        } catch (exception: SecurityException) {
            false
        } catch (exception: Exception) {
            false
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
        private val HOME_BACKGROUND_URI = stringPreferencesKey("home_background_uri")
    }
}

sealed interface SaveDestinationState {
    data object Default : SaveDestinationState
    data class Custom(val uriString: String, val folderName: String) : SaveDestinationState
    data class PermissionLost(val uriString: String, val folderName: String) : SaveDestinationState
    data class Unavailable(val uriString: String, val folderName: String) : SaveDestinationState
}

sealed interface HomeBackgroundUpdateResult {
    data object Saved : HomeBackgroundUpdateResult
    data object PermissionDenied : HomeBackgroundUpdateResult
}
