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

    fun toThemeMode(): AppThemeMode {
        return when (this) {
            System -> AppThemeMode.SocialSaver
            Light -> AppThemeMode.Light
            Dark -> AppThemeMode.Dark
        }
    }

    companion object {
        fun fromStorageKey(storageKey: String?): AppThemePreference {
            return entries.firstOrNull { it.storageKey == storageKey } ?: System
        }
    }
}