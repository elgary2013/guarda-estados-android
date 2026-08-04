package com.guardaestados.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guardaestados.R
import com.guardaestados.data.folder.FolderSelectionState
import com.guardaestados.data.settings.AppThemePreference

@Composable
fun SettingsScreen(
    folderSelectionState: FolderSelectionState,
    themePreference: AppThemePreference,
    appVersion: String,
    onSelectFolder: () -> Unit,
    onThemePreferenceSelected: (AppThemePreference) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.settings_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SettingsSection(title = stringResource(R.string.settings_folder_title)) {
                Text(
                    text = folderStatusText(folderSelectionState),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FolderDetail(folderSelectionState = folderSelectionState)
                Button(onClick = onSelectFolder) {
                    Text(text = folderActionText(folderSelectionState))
                }
            }

            SettingsSection(title = stringResource(R.string.settings_storage_title)) {
                Text(
                    text = stringResource(R.string.settings_storage_images),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.settings_storage_videos),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.settings_storage_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection(title = stringResource(R.string.settings_theme_title)) {
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

            SettingsSection(title = stringResource(R.string.settings_privacy_title)) {
                Text(
                    text = stringResource(R.string.settings_privacy_local),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.settings_privacy_folder_access),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.settings_legal_affiliation),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.settings_legal_user_responsibility),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            SettingsSection(title = stringResource(R.string.settings_about_title)) {
                Text(
                    text = stringResource(R.string.settings_about_app_name),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.settings_about_version, appVersion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun FolderDetail(folderSelectionState: FolderSelectionState) {
    val uriString = when (folderSelectionState) {
        is FolderSelectionState.PermissionLost -> folderSelectionState.uriString
        is FolderSelectionState.Selected -> folderSelectionState.uriString
        else -> null
    }
    if (uriString != null) {
        Text(
            text = stringResource(R.string.folder_selected_uri, uriString),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun folderStatusText(folderSelectionState: FolderSelectionState): String {
    return when (folderSelectionState) {
        FolderSelectionState.Loading -> stringResource(R.string.folder_status_loading)
        FolderSelectionState.NotSelected -> stringResource(R.string.folder_status_not_selected)
        is FolderSelectionState.PermissionLost -> stringResource(R.string.folder_status_permission_lost)
        is FolderSelectionState.Selected -> stringResource(R.string.folder_status_selected)
    }
}

@Composable
private fun folderActionText(folderSelectionState: FolderSelectionState): String {
    return when (folderSelectionState) {
        FolderSelectionState.NotSelected -> stringResource(R.string.folder_action_select)
        is FolderSelectionState.PermissionLost -> stringResource(R.string.folder_action_select_again)
        else -> stringResource(R.string.folder_action_change)
    }
}

@Composable
private fun ThemeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
