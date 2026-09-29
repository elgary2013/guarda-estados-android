package com.guardaestados.data.settings

import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppLanguageManager {
    @MainThread
    fun currentLanguage(): AppLanguage {
        val locale = AppCompatDelegate.getApplicationLocales()[0] ?: return AppLanguage.System
        return when (locale.language) {
            "es" -> AppLanguage.Spanish
            "en" -> AppLanguage.English
            "pt" -> AppLanguage.Portuguese
            else -> AppLanguage.System
        }
    }

    @MainThread
    fun applyLanguage(language: AppLanguage) {
        val locales = if (language == AppLanguage.System) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.languageTag)
        }
        if (AppCompatDelegate.getApplicationLocales() != locales) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
}
