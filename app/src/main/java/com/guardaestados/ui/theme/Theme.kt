package com.guardaestados.ui.theme

import android.app.Activity
import android.os.Build

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.guardaestados.data.settings.AppThemeMode

val LocalBrandGradientsEnabled = staticCompositionLocalOf { true }

data class GuardaEstadosColorTokens(
    val background: Color,
    val surface: Color,
    val surfaceStrong: Color,
    val surfaceSoft: Color,
    val title: Color,
    val body: Color,
    val border: Color,
    val active: Color,
    val activeAlt: Color,
    val thumbnailBackground: Color,
    val badgeOverlay: Color,
    val mediaBackground: Color,
    val danger: Color,
    val dangerSoft: Color,
    val dangerBorder: Color,
    val primaryGradient: Brush
)

private val DarkGuardaEstadosColors = GuardaEstadosColorTokens(
    background = SocialNightBackground,
    surface = SocialNightSurface,
    surfaceStrong = Color(0xE61A2421),
    surfaceSoft = SocialGreenSoft,
    title = SocialTextPrimary,
    body = SocialTextSecondary,
    border = SocialBorder,
    active = SocialGreen,
    activeAlt = SocialFuchsia,
    thumbnailBackground = Color(0xFF101A18),
    badgeOverlay = Color(0xD90A1210),
    mediaBackground = Color(0xFF050807),
    danger = SocialError,
    dangerSoft = Color(0x33FF6B5F),
    dangerBorder = Color(0x66FFB4AB),
    primaryGradient = Brush.horizontalGradient(listOf(SocialGreen, SocialFuchsia))
)

private val LightGuardaEstadosColors = GuardaEstadosColorTokens(
    background = NeutralLightBackground,
    surface = NeutralLightSurface,
    surfaceStrong = NeutralLightSurfaceHigh,
    surfaceSoft = Color(0x1F0F8F5A),
    title = NeutralLightTextPrimary,
    body = NeutralLightTextSecondary,
    border = NeutralLightBorder,
    active = Color(0xFF0F8F5A),
    activeAlt = Color(0xFF2D9C7A),
    thumbnailBackground = NeutralLightSurfaceHigh,
    badgeOverlay = Color(0xE6FFFFFF),
    mediaBackground = Color(0xFFE9EEF3),
    danger = NeutralLightError,
    dangerSoft = NeutralLightErrorContainer.copy(alpha = 0.55f),
    dangerBorder = NeutralLightError.copy(alpha = 0.35f),
    primaryGradient = Brush.horizontalGradient(listOf(Color(0xFF0F8F5A), Color(0xFF2D9C7A)))
)

val LocalGuardaEstadosColors = staticCompositionLocalOf { DarkGuardaEstadosColors }

private val SocialSaverColorScheme = darkColorScheme(
    primary = SocialGreen,
    onPrimary = SocialNightBackground,
    primaryContainer = SocialGreenSoft,
    onPrimaryContainer = SocialTextPrimary,
    secondary = SocialGreen,
    onSecondary = SocialTextPrimary,
    secondaryContainer = SocialNightSurfaceHigh,
    onSecondaryContainer = SocialTextPrimary,
    tertiary = SocialViolet,
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
    primaryContainer = SocialGreenSoft,
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
    drawHomePhotoBehindSystemBars: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.SocialSaver -> SocialSaverColorScheme
        AppThemeMode.Light -> NeutralLightColorScheme
        AppThemeMode.Dark -> NeutralDarkColorScheme
    }
    val appColors = when (themeMode) {
        AppThemeMode.Light -> LightGuardaEstadosColors
        AppThemeMode.SocialSaver,
        AppThemeMode.Dark -> DarkGuardaEstadosColors
    }
    val view = LocalView.current
    val useLightSystemBars = themeMode == AppThemeMode.Light

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val systemBarColor = if (drawHomePhotoBehindSystemBars) {
                Color.Transparent
            } else {
                colorScheme.background
            }

            window.statusBarColor = systemBarColor.toArgb()
            window.navigationBarColor = systemBarColor.toArgb()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = !drawHomePhotoBehindSystemBars
                window.isNavigationBarContrastEnforced = !drawHomePhotoBehindSystemBars
            }

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = useLightSystemBars && !drawHomePhotoBehindSystemBars
                isAppearanceLightNavigationBars = useLightSystemBars && !drawHomePhotoBehindSystemBars
            }
        }
    }

    CompositionLocalProvider(
        LocalBrandGradientsEnabled provides (themeMode == AppThemeMode.SocialSaver),
        LocalGuardaEstadosColors provides appColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
