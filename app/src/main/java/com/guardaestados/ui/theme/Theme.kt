package com.guardaestados.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = ArchiveBlue,
    onPrimary = ArchiveMist,
    primaryContainer = ArchiveBlueLight,
    onPrimaryContainer = ArchiveInk,
    secondary = ArchiveCoral,
    onSecondary = ArchiveMist,
    secondaryContainer = ArchiveCoralLight,
    onSecondaryContainer = ArchiveInk,
    background = ArchiveMist,
    onBackground = ArchiveInk,
    surface = ArchiveMist,
    onSurface = ArchiveInk,
    surfaceVariant = ArchivePanel,
    onSurfaceVariant = ColorOnSurfaceVariantLight,
    surfaceContainerHigh = ArchivePanel
)

private val DarkColorScheme = darkColorScheme(
    primary = NightBlue,
    onPrimary = NightBackground,
    primaryContainer = ArchiveBlue,
    onPrimaryContainer = NightInk,
    secondary = NightCoral,
    onSecondary = NightBackground,
    secondaryContainer = ArchiveCoral,
    onSecondaryContainer = NightInk,
    background = NightBackground,
    onBackground = NightInk,
    surface = NightBackground,
    onSurface = NightInk,
    surfaceVariant = NightPanel,
    onSurfaceVariant = ColorOnSurfaceVariantDark,
    surfaceContainerHigh = NightPanel
)

@Composable
fun GuardaEstadosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
