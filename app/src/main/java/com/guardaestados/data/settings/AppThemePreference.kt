package com.guardaestados.data.settings

enum class AppThemePreference(val storageKey: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        fun fromStorageKey(storageKey: String?): AppThemePreference {
            return entries.firstOrNull { it.storageKey == storageKey } ?: System
        }
    }
}
