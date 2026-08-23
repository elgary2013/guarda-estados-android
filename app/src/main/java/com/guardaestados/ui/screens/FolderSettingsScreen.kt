package com.guardaestados.ui.screens

import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guardaestados.R
import com.guardaestados.data.folder.FolderSelectionState
import com.guardaestados.ui.theme.BrandGradientButton
import com.guardaestados.ui.theme.LocalGuardaEstadosColors

private val FolderSettingsBackground: Color
    @Composable get() = LocalGuardaEstadosColors.current.background
private val FolderSettingsSurface: Color
    @Composable get() = LocalGuardaEstadosColors.current.surface
private val FolderSettingsSurfaceSoft: Color
    @Composable get() = LocalGuardaEstadosColors.current.surfaceSoft
private val FolderSettingsTitle: Color
    @Composable get() = LocalGuardaEstadosColors.current.title
private val FolderSettingsBody: Color
    @Composable get() = LocalGuardaEstadosColors.current.body
private val FolderSettingsBorder: Color
    @Composable get() = LocalGuardaEstadosColors.current.border
private val FolderSettingsActive: Color
    @Composable get() = LocalGuardaEstadosColors.current.active

@Composable
fun FolderSettingsScreen(
    folderSelectionState: FolderSelectionState,
    onSelectRecommendedFolder: () -> Unit,
    onSelectFolder: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = FolderSettingsBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FolderSettingsHeader(onBack = onBack)
            FolderSettingsSection(
                title = stringResource(R.string.settings_states_folder_title),
                icon = Icons.Filled.Folder
            ) {
                Text(
                    text = folderStatusText(folderSelectionState),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FolderSettingsBody
                )
                RecommendedFolderPath()
                when (folderSelectionState) {
                    is FolderSelectionState.Selected -> {
                        BrandGradientButton(
                            text = stringResource(R.string.folder_action_change),
                            onClick = onSelectFolder
                        )
                    }
                    else -> {
                        FolderSelectionGuide()
                        Text(
                            text = stringResource(R.string.folder_selection_xiaomi_note),
                            style = MaterialTheme.typography.bodyMedium,
                            color = FolderSettingsBody
                        )
                        BrandGradientButton(
                            text = stringResource(R.string.folder_action_open_recommended),
                            onClick = onSelectRecommendedFolder
                        )
                        TextButton(onClick = onSelectFolder) {
                            Text(
                                text = stringResource(R.string.folder_action_select_manual),
                                color = FolderSettingsActive
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderSettingsHeader(onBack: () -> Unit) {
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
                tint = FolderSettingsTitle
            )
        }
        Text(
            text = stringResource(R.string.settings_states_folder_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = FolderSettingsTitle
        )
    }
}

@Composable
private fun FolderSettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FolderSettingsSurface),
        border = BorderStroke(1.dp, FolderSettingsBorder)
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
                FolderSettingsIcon(icon = icon)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = FolderSettingsTitle
                )
            }
            content()
        }
    }
}

@Composable
private fun FolderSettingsIcon(icon: ImageVector) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = FolderSettingsSurfaceSoft,
        contentColor = FolderSettingsActive,
        border = BorderStroke(1.dp, FolderSettingsBorder)
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
                tint = FolderSettingsActive
            )
        }
    }
}

@Composable
private fun FolderSelectionGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.folder_selection_steps_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = FolderSettingsTitle
        )
        Text(text = stringResource(R.string.folder_selection_step_open_recommended), color = FolderSettingsBody)
        Text(text = stringResource(R.string.folder_selection_step_open_statuses), color = FolderSettingsBody)
        Text(text = stringResource(R.string.folder_selection_step_confirm), color = FolderSettingsBody)
    }
}

@Composable
private fun RecommendedFolderPath() {
    Text(
        text = stringResource(R.string.settings_folder_recommended_path_title),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = FolderSettingsTitle
    )
    Text(
        text = stringResource(R.string.settings_folder_recommended_path),
        style = MaterialTheme.typography.bodyMedium,
        color = FolderSettingsBody
    )
}

@Composable
private fun folderStatusText(folderSelectionState: FolderSelectionState): String {
    return when (folderSelectionState) {
        FolderSelectionState.Loading -> stringResource(R.string.folder_status_loading)
        FolderSelectionState.NotSelected -> stringResource(R.string.folder_status_not_connected)
        is FolderSelectionState.PermissionLost -> stringResource(R.string.folder_status_permission_lost_reconnect)
        is FolderSelectionState.Selected -> {
            if (folderSelectionState.uriString.isRecommendedStatusesFolder()) {
                stringResource(
                    R.string.folder_status_connected_recommended,
                    stringResource(R.string.settings_folder_statuses_name)
                )
            } else {
                stringResource(R.string.folder_status_connected_manual)
            }
        }
    }
}

private fun String.isRecommendedStatusesFolder(): Boolean {
    val uri = runCatching { Uri.parse(this) }.getOrNull() ?: return false
    return runCatching { DocumentsContract.getTreeDocumentId(uri) }
        .getOrNull()
        .equals(RecommendedStatusesDocumentId, ignoreCase = true)
}

private const val RecommendedStatusesDocumentId =
    "primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
