package com.guardaestados.data.settings

enum class AppThemeMode {
    SocialSaver,
    Light,
    Dark
}

enum class AppThemePreference(val storageKey: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    fun toThemeMode(systemDarkTheme: Boolean): AppThemeMode {
        return when (this) {
            System -> if (systemDarkTheme) AppThemeMode.SocialSaver else AppThemeMode.Light
            Light -> AppThemeMode.Light
            Dark -> AppThemeMode.SocialSaver
        }
    }

    companion object {
        fun fromStorageKey(storageKey: String?): AppThemePreference {
            return entries.firstOrNull { it.storageKey == storageKey } ?: System
        }
    }
}