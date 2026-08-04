package com.guardaestados.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsRepository(context: Context) {
    private val appContext = context.applicationContext

    val themePreference: Flow<AppThemePreference> = appContext.appSettingsDataStore.data
        .map { preferences ->
            AppThemePreference.fromStorageKey(preferences[THEME_PREFERENCE])
        }
        .catch { emit(AppThemePreference.System) }

    suspend fun saveThemePreference(themePreference: AppThemePreference) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[THEME_PREFERENCE] = themePreference.storageKey
        }
    }

    companion object {
        private val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    }
}
