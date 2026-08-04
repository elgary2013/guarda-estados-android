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
}
