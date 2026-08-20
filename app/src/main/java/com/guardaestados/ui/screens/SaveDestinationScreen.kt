package com.guardaestados.ui.screens

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
import com.guardaestados.data.settings.SaveDestinationState
import com.guardaestados.ui.theme.BrandGradientButton
import com.guardaestados.ui.theme.LocalGuardaEstadosColors

private val DestinationBackground: Color
    @Composable get() = LocalGuardaEstadosColors.current.background
private val DestinationSurface: Color
    @Composable get() = LocalGuardaEstadosColors.current.surface
private val DestinationSurfaceSoft: Color
    @Composable get() = LocalGuardaEstadosColors.current.surfaceSoft
private val DestinationTitle: Color
    @Composable get() = LocalGuardaEstadosColors.current.title
private val DestinationBody: Color
    @Composable get() = LocalGuardaEstadosColors.current.body
private val DestinationBorder: Color
    @Composable get() = LocalGuardaEstadosColors.current.border
private val DestinationActive: Color
    @Composable get() = LocalGuardaEstadosColors.current.active

@Composable
fun SaveDestinationScreen(
    saveDestinationState: SaveDestinationState,
    onSelectSaveDestination: () -> Unit,
    onUseDefaultSaveDestination: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = DestinationBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SaveDestinationHeader(onBack = onBack)
            SaveDestinationSection(
                title = stringResource(R.string.settings_save_destination_current_title),
                icon = Icons.Filled.Folder
            ) {
                SaveDestinationStatus(saveDestinationState = saveDestinationState)
                Text(
                    text = stringResource(R.string.settings_save_destination_new_copies_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DestinationBody
                )
                BrandGradientButton(
                    text = stringResource(R.string.settings_save_destination_change),
                    onClick = onSelectSaveDestination
                )
                if (saveDestinationState != SaveDestinationState.Default) {
                    TextButton(onClick = onUseDefaultSaveDestination) {
                        Text(
                            text = stringResource(R.string.settings_save_destination_default_action),
                            color = DestinationActive
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveDestinationHeader(onBack: () -> Unit) {
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
                tint = DestinationTitle
            )
        }
        Text(
            text = stringResource(R.string.settings_save_destination_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = DestinationTitle
        )
    }
}

@Composable
private fun SaveDestinationSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DestinationSurface),
        border = BorderStroke(1.dp, DestinationBorder)
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
                SaveDestinationIcon(icon = icon)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DestinationTitle
                )
            }
            content()
        }
    }
}

@Composable
private fun SaveDestinationIcon(icon: ImageVector) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = DestinationSurfaceSoft,
        contentColor = DestinationActive,
        border = BorderStroke(1.dp, DestinationBorder)
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
                tint = DestinationActive
            )
        }
    }
}

@Composable
private fun SaveDestinationStatus(saveDestinationState: SaveDestinationState) {
    when (saveDestinationState) {
        SaveDestinationState.Default -> DefaultDestinationDetails()
        is SaveDestinationState.Custom -> DestinationStatusText(
            text = stringResource(
                R.string.settings_save_destination_custom,
                saveDestinationState.folderName
            )
        )
        is SaveDestinationState.PermissionLost -> DestinationStatusText(
            text = stringResource(
                R.string.settings_save_destination_permission_lost,
                saveDestinationState.folderName
            )
        )
        is SaveDestinationState.Unavailable -> DestinationStatusText(
            text = stringResource(
                R.string.settings_save_destination_unavailable,
                saveDestinationState.folderName
            )
        )
    }
}

@Composable
private fun DefaultDestinationDetails() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DestinationStatusText(
            text = stringResource(R.string.settings_save_destination_default_title)
        )
        DestinationPathText(
            text = stringResource(R.string.settings_save_destination_default_images)
        )
        DestinationPathText(
            text = stringResource(R.string.settings_save_destination_default_videos)
        )
    }
}

@Composable
private fun DestinationStatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = DestinationTitle
    )
}

@Composable
private fun DestinationPathText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = DestinationBody
    )
}
