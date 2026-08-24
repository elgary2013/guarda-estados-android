package com.guardaestados.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guardaestados.R
import com.guardaestados.data.settings.AppThemePreference
import com.guardaestados.data.settings.IncludedHomeBackground
import com.guardaestados.ui.theme.BrandGradientButton
import com.guardaestados.ui.theme.EstadoGoIncludedHomeBackground
import com.guardaestados.ui.theme.LocalGuardaEstadosColors

private val AppearanceBackground: Color
    @Composable get() = LocalGuardaEstadosColors.current.background
private val AppearanceSurface: Color
    @Composable get() = LocalGuardaEstadosColors.current.surface
private val AppearanceSurfaceSoft: Color
    @Composable get() = LocalGuardaEstadosColors.current.surfaceSoft
private val AppearanceTitle: Color
    @Composable get() = LocalGuardaEstadosColors.current.title
private val AppearanceBody: Color
    @Composable get() = LocalGuardaEstadosColors.current.body
private val AppearanceBorder: Color
    @Composable get() = LocalGuardaEstadosColors.current.border
private val AppearanceActive: Color
    @Composable get() = LocalGuardaEstadosColors.current.active
private val AppearanceDanger: Color
    @Composable get() = LocalGuardaEstadosColors.current.danger

@Composable
fun AppearanceScreen(
    themePreference: AppThemePreference,
    homeBackgroundUri: String?,
    includedHomeBackground: IncludedHomeBackground?,
    onThemePreferenceSelected: (AppThemePreference) -> Unit,
    onSelectHomeBackground: () -> Unit,
    onClearHomeBackground: () -> Unit,
    onSelectIncludedHomeBackground: (IncludedHomeBackground) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = AppearanceBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppearanceHeader(onBack = onBack)
            AppearanceSection(
                title = stringResource(R.string.settings_theme_title),
                icon = Icons.Filled.Palette
            ) {
                AppearanceStatusText(
                    text = stringResource(
                        R.string.appearance_theme_selected,
                        selectedThemeText(themePreference)
                    )
                )
                ThemeOption(
                    text = stringResource(R.string.settings_theme_system),
                    selected = themePreference == AppThemePreference.System,
                    onClick = { onThemePreferenceSelected(AppThemePreference.System) }
                )
                ThemeOption(
                    text = stringResource(R.string.settings_theme_light),
                    selected = themePreference == AppThemePreference.Light,
                    onClick = { onThemePreferenceSelected(AppThemePreference.Light) }
                )
                ThemeOption(
                    text = stringResource(R.string.settings_theme_dark),
                    selected = themePreference == AppThemePreference.Dark,
                    onClick = { onThemePreferenceSelected(AppThemePreference.Dark) }
                )
            }
            AppearanceSection(
                title = stringResource(R.string.settings_home_background_title),
                icon = Icons.Filled.Image
            ) {
                Text(
                    text = homeBackgroundStatusText(homeBackgroundUri, includedHomeBackground),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppearanceTitle
                )
                BrandGradientButton(
                    text = stringResource(
                        if (homeBackgroundUri == null) {
                            R.string.appearance_home_background_choose
                        } else {
                            R.string.appearance_home_background_change
                        }
                    ),
                    onClick = onSelectHomeBackground
                )
                if (homeBackgroundUri != null || includedHomeBackground != null) {
                    TextButton(onClick = onClearHomeBackground) {
                        Text(
                            text = stringResource(R.string.settings_home_background_use_default),
                            color = AppearanceDanger
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.settings_home_background_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppearanceBody
                )
            }
            AppearanceSection(
                title = stringResource(R.string.appearance_included_backgrounds_title),
                icon = Icons.Filled.Image
            ) {
                Text(
                    text = stringResource(R.string.appearance_included_backgrounds_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppearanceBody
                )
                IncludedHomeBackground.entries.forEach { background ->
                    IncludedBackgroundOption(
                        background = background,
                        selected = homeBackgroundUri == null && includedHomeBackground == background,
                        onClick = { onSelectIncludedHomeBackground(background) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceStatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = AppearanceTitle
    )
}

@Composable
private fun AppearanceHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.preview_action_back),
                tint = AppearanceTitle
            )
        }
        Text(
            text = stringResource(R.string.settings_appearance_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppearanceTitle
        )
    }
}

@Composable
private fun AppearanceSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppearanceSurface),
        border = BorderStroke(1.dp, AppearanceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppearanceSectionIcon(icon = icon)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppearanceTitle
                )
            }
            content()
        }
    }
}

@Composable
private fun AppearanceSectionIcon(icon: ImageVector) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = AppearanceSurfaceSoft,
        contentColor = AppearanceActive,
        border = BorderStroke(1.dp, AppearanceBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = AppearanceActive
            )
        }
    }
}

@Composable
private fun ThemeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) AppearanceSurfaceSoft else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = backgroundColor,
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = AppearanceActive,
                        unselectedColor = AppearanceBody
                    )
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = AppearanceTitle
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun IncludedBackgroundOption(
    background: IncludedHomeBackground,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) AppearanceSurfaceSoft else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = backgroundColor,
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(width = 72.dp, height = 46.dp),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, AppearanceBorder)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        EstadoGoIncludedHomeBackground(
                            background = background,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(background.titleRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = AppearanceTitle
                    )
                    Text(
                        text = stringResource(R.string.appearance_included_background_status),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppearanceBody
                    )
                }
                RadioButton(
                    selected = selected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = AppearanceActive,
                        unselectedColor = AppearanceBody
                    )
                )
            }
        }
    }
}

@Composable
private fun homeBackgroundStatusText(
    homeBackgroundUri: String?,
    includedHomeBackground: IncludedHomeBackground?
): String {
    return when {
        homeBackgroundUri != null -> stringResource(R.string.appearance_home_background_status_custom)
        includedHomeBackground != null -> stringResource(
            R.string.appearance_home_background_status_included,
            stringResource(includedHomeBackground.titleRes())
        )
        else -> stringResource(R.string.appearance_home_background_status_default)
    }
}

@Composable
private fun selectedThemeText(themePreference: AppThemePreference): String {
    return when (themePreference) {
        AppThemePreference.System -> stringResource(R.string.settings_theme_system)
        AppThemePreference.Light -> stringResource(R.string.settings_theme_light)
        AppThemePreference.Dark -> stringResource(R.string.settings_theme_dark)
    }
}

@StringRes
private fun IncludedHomeBackground.titleRes(): Int {
    return when (this) {
        IncludedHomeBackground.AuraGreen -> R.string.appearance_background_aura_green
        IncludedHomeBackground.EmeraldWaves -> R.string.appearance_background_emerald_waves
        IncludedHomeBackground.LuminousNight -> R.string.appearance_background_luminous_night
    }
}
