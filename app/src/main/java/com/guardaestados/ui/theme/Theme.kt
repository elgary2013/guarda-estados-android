package com.guardaestados.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SocialSaverColorScheme = darkColorScheme(
    primary = SocialGreen,
    onPrimary = SocialNightBackground,
    primaryContainer = SocialNightSurfaceHighest,
    onPrimaryContainer = SocialTextPrimary,
    secondary = SocialViolet,
    onSecondary = SocialTextPrimary,
    secondaryContainer = SocialNightSurfaceHigh,
    onSecondaryContainer = SocialTextPrimary,
    tertiary = SocialFuchsia,
    onTertiary = SocialTextPrimary,
    background = SocialNightBackground,
    onBackground = SocialTextPrimary,
    surface = SocialNightSurface,
    onSurface = SocialTextPrimary,
    surfaceVariant = SocialNightSurfaceHigh,
    onSurfaceVariant = SocialTextSecondary,
    surfaceContainerLowest = SocialNightBackground,
    surfaceContainerLow = SocialNightSurface,
    surfaceContainer = SocialNightSurface,
    surfaceContainerHigh = SocialNightSurfaceHigh,
    surfaceContainerHighest = SocialNightSurfaceHighest,
    outline = SocialBorder,
    outlineVariant = SocialBorder,
    error = SocialError,
    onError = SocialOnError,
    errorContainer = SocialErrorContainer,
    onErrorContainer = SocialOnErrorContainer
)

@Composable
fun GuardaEstadosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SocialSaverColorScheme,
        typography = Typography,
        content = content
    )
}
