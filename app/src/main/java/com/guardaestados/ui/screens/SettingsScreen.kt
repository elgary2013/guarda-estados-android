package com.guardaestados.ui.screens

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guardaestados.R
import com.guardaestados.data.folder.FolderSelectionState
import com.guardaestados.data.settings.AppThemePreference
import com.guardaestados.data.settings.SaveDestinationState
import com.guardaestados.ui.settings.SettingsResetState
import com.guardaestados.ui.theme.BrandGradientButton
import com.guardaestados.ui.theme.brandGradientBorder

@Composable
fun SettingsScreen(
    folderSelectionState: FolderSelectionState,
    themePreference: AppThemePreference,
    saveDestinationState: SaveDestinationState,
    appVersion: String,
    onSelectFolder: () -> Unit,
    onSelectSaveDestination: () -> Unit,
    onUseDefaultSaveDestination: () -> Unit,
    onThemePreferenceSelected: (AppThemePreference) -> Unit,
    resetState: SettingsResetState,
    onResetSettings: () -> Unit,
    onResetMessageDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var expandedSection by remember { mutableStateOf<SettingsSectionKey?>(null) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ExpandableSettingsSection(
                title = stringResource(R.string.settings_folder_title),
                summary = folderSummaryText(folderSelectionState),
                expanded = expandedSection == SettingsSectionKey.SourceFolder,
                onToggle = { expandedSection = expandedSection.toggle(SettingsSectionKey.SourceFolder) }
            ) {
                Text(
                    text = folderStatusText(folderSelectionState),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.settings_folder_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FolderDetail(folderSelectionState = folderSelectionState)
                BrandGradientButton(
                    text = folderActionText(folderSelectionState),
                    onClick = onSelectFolder
                )
            }

            ExpandableSettingsSection(
                title = stringResource(R.string.settings_save_destination_title),
                summary = saveDestinationSummaryText(saveDestinationState),
                expanded = expandedSection == SettingsSectionKey.SaveDestination,
                onToggle = { expandedSection = expandedSection.toggle(SettingsSectionKey.SaveDestination) }
            ) {
                Text(
                    text = saveDestinationStatusText(saveDestinationState),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.settings_save_destination_new_copies_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BrandGradientButton(
                    text = stringResource(R.string.settings_save_destination_change),
                    onClick = onSelectSaveDestination
                )
                TextButton(onClick = onUseDefaultSaveDestination) {
                    Text(text = stringResource(R.string.settings_save_destination_default_action))
                }
            }

            ExpandableSettingsSection(
                title = stringResource(R.string.settings_appearance_title),
                summary = themeSummaryText(themePreference),
                expanded = expandedSection == SettingsSectionKey.Appearance,
                onToggle = { expandedSection = expandedSection.toggle(SettingsSectionKey.Appearance) }
            ) {
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

            ExpandableSettingsSection(
                title = stringResource(R.string.settings_privacy_info_title),
                summary = stringResource(R.string.settings_privacy_info_summary),
                expanded = expandedSection == SettingsSectionKey.PrivacyInfo,
                onToggle = { expandedSection = expandedSection.toggle(SettingsSectionKey.PrivacyInfo) }
            ) {
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
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = stringResource(R.string.settings_about_app_name),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.settings_about_version, appVersion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ExpandableSettingsSection(
                title = stringResource(R.string.settings_reset_title),
                summary = stringResource(R.string.settings_reset_summary),
                expanded = expandedSection == SettingsSectionKey.Reset,
                onToggle = { expandedSection = expandedSection.toggle(SettingsSectionKey.Reset) }
            ) {
                Text(
                    text = stringResource(R.string.settings_reset_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.settings_reset_keeps_copies),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { showResetDialog = true },
                    enabled = resetState != SettingsResetState.Resetting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(text = stringResource(R.string.settings_reset_action))
                }
                if (resetState == SettingsResetState.Success) {
                    ResetSuccessMessage(onDismiss = onResetMessageDismissed)
                }
            }
        }
    }

    if (showResetDialog) {
        ResetSettingsDialog(
            isResetting = resetState == SettingsResetState.Resetting,
            onDismiss = { showResetDialog = false },
            onConfirm = {
                showResetDialog = false
                onResetSettings()
            }
        )
    }
}

@Composable
private fun ExpandableSettingsSection(
    title: String,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = brandGradientBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onToggle)
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SettingsChevron(expanded = expanded)
            }
            if (expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun SettingsChevron(expanded: Boolean) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "settings-chevron")
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        modifier = Modifier
            .size(28.dp)
            .graphicsLayer(rotationZ = rotation)
    ) {
        val strokeWidth = 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.25f, size.height * 0.38f),
            end = Offset(size.width * 0.5f, size.height * 0.62f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.75f, size.height * 0.38f),
            end = Offset(size.width * 0.5f, size.height * 0.62f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun ResetSettingsDialog(
    isResetting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_reset_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.settings_reset_dialog_body))
                Text(text = stringResource(R.string.settings_reset_dialog_keep_copies))
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isResetting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(text = stringResource(R.string.settings_reset_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isResetting
            ) {
                Text(text = stringResource(R.string.settings_reset_dialog_cancel))
            }
        }
    )
}

@Composable
private fun ResetSuccessMessage(onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_reset_success),
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.settings_reset_success_dismiss))
            }
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
    val folderName = uriString?.friendlyFolderName()
    if (folderName != null) {
        Text(
            text = stringResource(R.string.settings_folder_name, folderName),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
    if (uriString != null) {
        Text(
            text = stringResource(R.string.folder_selected_uri, uriString),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun folderSummaryText(folderSelectionState: FolderSelectionState): String {
    return when (folderSelectionState) {
        FolderSelectionState.Loading -> stringResource(R.string.folder_status_loading)
        is FolderSelectionState.Selected -> stringResource(R.string.settings_folder_summary_selected)
        is FolderSelectionState.PermissionLost -> stringResource(R.string.folder_status_permission_lost)
        FolderSelectionState.NotSelected -> stringResource(R.string.settings_folder_summary_empty)
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
private fun saveDestinationSummaryText(saveDestinationState: SaveDestinationState): String {
    return when (saveDestinationState) {
        SaveDestinationState.Default -> stringResource(R.string.settings_save_destination_summary_default)
        is SaveDestinationState.Custom -> saveDestinationState.folderName
        is SaveDestinationState.PermissionLost -> stringResource(R.string.settings_save_destination_summary_attention)
        is SaveDestinationState.Unavailable -> stringResource(R.string.settings_save_destination_summary_attention)
    }
}

@Composable
private fun saveDestinationStatusText(saveDestinationState: SaveDestinationState): String {
    return when (saveDestinationState) {
        SaveDestinationState.Default -> stringResource(R.string.settings_save_destination_default)
        is SaveDestinationState.Custom -> stringResource(
            R.string.settings_save_destination_custom,
            saveDestinationState.folderName
        )
        is SaveDestinationState.PermissionLost -> stringResource(
            R.string.settings_save_destination_permission_lost,
            saveDestinationState.folderName
        )
        is SaveDestinationState.Unavailable -> stringResource(
            R.string.settings_save_destination_unavailable,
            saveDestinationState.folderName
        )
    }
}

@Composable
private fun themeSummaryText(themePreference: AppThemePreference): String {
    return when (themePreference) {
        AppThemePreference.System -> stringResource(R.string.settings_theme_summary_system)
        AppThemePreference.Light -> stringResource(R.string.settings_theme_summary_light)
        AppThemePreference.Dark -> stringResource(R.string.settings_theme_summary_dark)
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
            .padding(vertical = 6.dp),
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

private fun SettingsSectionKey?.toggle(section: SettingsSectionKey): SettingsSectionKey? {
    return if (this == section) null else section
}

private fun String.friendlyFolderName(): String {
    val decoded = Uri.decode(this)
    return decoded
        .substringBefore('?')
        .substringAfterLast(':')
        .substringAfterLast('/')
        .ifBlank { this }
}

private enum class SettingsSectionKey {
    SourceFolder,
    SaveDestination,
    Appearance,
    PrivacyInfo,
    Reset
}