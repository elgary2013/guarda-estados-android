package com.guardaestados.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guardaestados.R
import com.guardaestados.data.folder.FolderSelectionState

@Composable
fun HomeScreen(
    folderSelectionState: FolderSelectionState,
    onSelectFolder: () -> Unit,
    onOpenStates: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FolderStatusCard(
                folderSelectionState = folderSelectionState,
                onSelectFolder = onSelectFolder,
                onOpenStates = onOpenStates
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.home_placeholder_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FolderStatusCard(
    folderSelectionState: FolderSelectionState,
    onSelectFolder: () -> Unit,
    onOpenStates: () -> Unit
) {
    val containerColor = when (folderSelectionState) {
        is FolderSelectionState.PermissionLost -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.home_summary_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            when (folderSelectionState) {
                FolderSelectionState.Loading -> Text(
                    text = stringResource(R.string.folder_status_loading),
                    style = MaterialTheme.typography.bodyMedium
                )

                FolderSelectionState.NotSelected -> FolderActionRow(
                    text = stringResource(R.string.folder_status_not_selected),
                    actionText = stringResource(R.string.folder_action_select),
                    onClick = onSelectFolder
                )

                is FolderSelectionState.PermissionLost -> FolderActionRow(
                    text = stringResource(R.string.folder_status_permission_lost),
                    actionText = stringResource(R.string.folder_action_select_again),
                    onClick = onSelectFolder
                )

                is FolderSelectionState.Selected -> FolderActionRow(
                    text = stringResource(R.string.folder_status_selected),
                    actionText = stringResource(R.string.home_primary_action),
                    onClick = onOpenStates,
                    secondaryText = folderSelectionState.uriString
                )
            }
        }
    }
}

@Composable
private fun FolderActionRow(
    text: String,
    actionText: String,
    onClick: () -> Unit,
    secondaryText: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(onClick = onClick) {
                Text(text = actionText)
            }
        }
        if (secondaryText != null) {
            Text(
                text = stringResource(R.string.folder_selected_uri, secondaryText),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
