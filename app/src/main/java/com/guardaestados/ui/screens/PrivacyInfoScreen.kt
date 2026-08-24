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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import com.guardaestados.ui.theme.LocalGuardaEstadosColors

private val PrivacyBackground: Color
    @Composable get() = LocalGuardaEstadosColors.current.background
private val PrivacySurface: Color
    @Composable get() = LocalGuardaEstadosColors.current.surface
private val PrivacySurfaceSoft: Color
    @Composable get() = LocalGuardaEstadosColors.current.surfaceSoft
private val PrivacyTitle: Color
    @Composable get() = LocalGuardaEstadosColors.current.title
private val PrivacyBody: Color
    @Composable get() = LocalGuardaEstadosColors.current.body
private val PrivacyBorder: Color
    @Composable get() = LocalGuardaEstadosColors.current.border
private val PrivacyActive: Color
    @Composable get() = LocalGuardaEstadosColors.current.active

@Composable
fun PrivacyInfoScreen(
    appVersion: String,
    onOpenPrivacyPolicy: () -> Unit,
    onShareApp: () -> Unit,
    onRateApp: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = PrivacyBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PrivacyHeader(onBack = onBack)
            PrivacySection(
                title = stringResource(R.string.settings_privacy_info_title),
                icon = Icons.Filled.Info
            ) {
                PrivacyInfoBlock(
                    title = stringResource(R.string.privacy_info_data_title),
                    body = stringResource(R.string.privacy_info_data_body)
                )
                PrivacyInfoBlock(
                    title = stringResource(R.string.privacy_info_folder_title),
                    body = stringResource(R.string.privacy_info_folder_body)
                )
                PrivacyInfoBlock(
                    title = stringResource(R.string.privacy_info_saved_copies_title),
                    body = stringResource(R.string.privacy_info_saved_copies_body)
                )
                PrivacyInfoBlock(
                    title = stringResource(R.string.privacy_info_no_access_title),
                    body = stringResource(R.string.privacy_info_no_access_body)
                )
                PrivacyInfoBlock(
                    title = stringResource(R.string.privacy_info_responsible_use_title),
                    body = stringResource(R.string.privacy_info_responsible_use_body)
                )
                PrivacyInfoBlock(
                    title = stringResource(R.string.privacy_info_app_title),
                    body = stringResource(
                        R.string.privacy_info_app_body,
                        stringResource(R.string.app_name),
                        appVersion
                    )
                )
                PrivacyActionButton(
                    text = stringResource(R.string.privacy_info_share_app),
                    icon = Icons.Filled.Share,
                    onClick = onShareApp
                )
                PrivacyActionButton(
                    text = stringResource(R.string.privacy_info_rate_app),
                    icon = Icons.Filled.Star,
                    onClick = onRateApp
                )
                PrivacyPolicyButton(onClick = onOpenPrivacyPolicy)
            }
        }
    }
}

@Composable
private fun PrivacyHeader(onBack: () -> Unit) {
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
                tint = PrivacyTitle
            )
        }
        Text(
            text = stringResource(R.string.settings_privacy_info_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = PrivacyTitle
        )
    }
}

@Composable
private fun PrivacyInfoBlock(
    title: String,
    body: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = PrivacyTitle
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = PrivacyBody
        )
    }
}

@Composable
private fun PrivacySection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PrivacySurface),
        border = BorderStroke(1.dp, PrivacyBorder)
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
                PrivacyIcon(icon = icon)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrivacyTitle
                )
            }
            content()
        }
    }
}

@Composable
private fun PrivacyIcon(icon: ImageVector) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = PrivacySurfaceSoft,
        contentColor = PrivacyActive,
        border = BorderStroke(1.dp, PrivacyBorder)
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
                tint = PrivacyActive
            )
        }
    }
}

@Composable
private fun PrivacyActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                color = PrivacyActive,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = PrivacyActive
            )
        }
    }
}

@Composable
private fun PrivacyPolicyButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_privacy_policy),
                modifier = Modifier.weight(1f),
                color = PrivacyActive,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = PrivacyActive
            )
        }
    }
}
