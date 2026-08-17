package com.guardaestados.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppThemePreferenceTest {
    @Test
    fun `maps stored keys to theme preferences`() {
        assertEquals(AppThemePreference.System, AppThemePreference.fromStorageKey("system"))
        assertEquals(AppThemePreference.Light, AppThemePreference.fromStorageKey("light"))
        assertEquals(AppThemePreference.Dark, AppThemePreference.fromStorageKey("dark"))
    }

    @Test
    fun `falls back to system theme for missing or unknown keys`() {
        assertEquals(AppThemePreference.System, AppThemePreference.fromStorageKey(null))
        assertEquals(AppThemePreference.System, AppThemePreference.fromStorageKey(""))
        assertEquals(AppThemePreference.System, AppThemePreference.fromStorageKey("unexpected"))
    }

    @Test
    fun `system preference follows system theme mode`() {
        assertEquals(AppThemeMode.Light, AppThemePreference.System.toThemeMode(systemDarkTheme = false))
        assertEquals(AppThemeMode.SocialSaver, AppThemePreference.System.toThemeMode(systemDarkTheme = true))
    }

    @Test
    fun `light preference resolves to light theme mode`() {
        assertEquals(AppThemeMode.Light, AppThemePreference.Light.toThemeMode(systemDarkTheme = true))
    }

    @Test
    fun `dark preference resolves to socialsaver theme mode`() {
        assertEquals(AppThemeMode.SocialSaver, AppThemePreference.Dark.toThemeMode(systemDarkTheme = false))
    }
}