package com.guardaestados.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.guardaestados.data.settings.AppThemeMode

val LocalBrandGradientsEnabled = staticCompositionLocalOf { true }

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

private val NeutralLightColorScheme = lightColorScheme(
    primary = NeutralLightPrimary,
    onPrimary = NeutralLightOnPrimary,
    primaryContainer = NeutralLightSurfaceHighest,
    onPrimaryContainer = NeutralLightTextPrimary,
    secondary = NeutralLightSecondary,
    onSecondary = NeutralLightOnPrimary,
    secondaryContainer = NeutralLightSurfaceHigh,
    onSecondaryContainer = NeutralLightTextPrimary,
    tertiary = NeutralLightSecondary,
    onTertiary = NeutralLightOnPrimary,
    background = NeutralLightBackground,
    onBackground = NeutralLightTextPrimary,
    surface = NeutralLightSurface,
    onSurface = NeutralLightTextPrimary,
    surfaceVariant = NeutralLightSurfaceHigh,
    onSurfaceVariant = NeutralLightTextSecondary,
    surfaceContainerLowest = NeutralLightSurface,
    surfaceContainerLow = NeutralLightBackground,
    surfaceContainer = NeutralLightSurface,
    surfaceContainerHigh = NeutralLightSurfaceHigh,
    surfaceContainerHighest = NeutralLightSurfaceHighest,
    outline = NeutralLightBorder,
    outlineVariant = NeutralLightBorder,
    error = NeutralLightError,
    onError = NeutralLightOnError,
    errorContainer = NeutralLightErrorContainer,
    onErrorContainer = NeutralLightOnErrorContainer
)

private val NeutralDarkColorScheme = darkColorScheme(
    primary = NeutralDarkPrimary,
    onPrimary = NeutralDarkOnPrimary,
    primaryContainer = NeutralDarkSurfaceHighest,
    onPrimaryContainer = NeutralDarkTextPrimary,
    secondary = NeutralDarkSecondary,
    onSecondary = NeutralDarkOnPrimary,
    secondaryContainer = NeutralDarkSurfaceHigh,
    onSecondaryContainer = NeutralDarkTextPrimary,
    tertiary = NeutralDarkSecondary,
    onTertiary = NeutralDarkOnPrimary,
    background = NeutralDarkBackground,
    onBackground = NeutralDarkTextPrimary,
    surface = NeutralDarkSurface,
    onSurface = NeutralDarkTextPrimary,
    surfaceVariant = NeutralDarkSurfaceHigh,
    onSurfaceVariant = NeutralDarkTextSecondary,
    surfaceContainerLowest = NeutralDarkBackground,
    surfaceContainerLow = NeutralDarkSurface,
    surfaceContainer = NeutralDarkSurface,
    surfaceContainerHigh = NeutralDarkSurfaceHigh,
    surfaceContainerHighest = NeutralDarkSurfaceHighest,
    outline = NeutralDarkBorder,
    outlineVariant = NeutralDarkBorder,
    error = NeutralDarkError,
    onError = NeutralDarkOnError,
    errorContainer = NeutralDarkErrorContainer,
    onErrorContainer = NeutralDarkOnErrorContainer
)

@Composable
fun GuardaEstadosTheme(
    themeMode: AppThemeMode = AppThemeMode.SocialSaver,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.SocialSaver -> SocialSaverColorScheme
        AppThemeMode.Light -> NeutralLightColorScheme
        AppThemeMode.Dark -> NeutralDarkColorScheme
    }
    CompositionLocalProvider(
        LocalBrandGradientsEnabled provides (themeMode == AppThemeMode.SocialSaver)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}