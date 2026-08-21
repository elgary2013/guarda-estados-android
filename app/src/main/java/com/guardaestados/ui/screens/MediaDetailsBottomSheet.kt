package com.guardaestados.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guardaestados.R
import com.guardaestados.domain.media.MediaDetails
import com.guardaestados.domain.media.MediaDetailsFormatter
import com.guardaestados.domain.media.MediaDetailsOrigin
import com.guardaestados.domain.media.MediaDetailsType
import com.guardaestados.ui.media.MediaDetailsUiState
import com.guardaestados.ui.theme.LocalGuardaEstadosColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailsBottomSheet(
    state: MediaDetailsUiState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGuardaEstadosColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = colors.surface,
        contentColor = colors.title
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.media_details_sheet_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.title
                )
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.media_details_close))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                when (state) {
                    MediaDetailsUiState.Idle,
                    MediaDetailsUiState.Loading -> MediaDetailsLoading()
                    is MediaDetailsUiState.Content -> MediaDetailsContent(details = state.details)
                }
            }
        }
    }
}

@Composable
private fun MediaDetailsLoading() {
    val colors = LocalGuardaEstadosColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.dp,
            color = colors.active
        )
        Text(
            text = stringResource(R.string.media_details_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.body
        )
    }
}

@Composable
private fun MediaDetailsContent(details: MediaDetails) {
    val formatter = remember { MediaDetailsFormatter() }
    val unavailable = stringResource(R.string.media_details_unavailable)
    val type = stringResource(
        if (details.type == MediaDetailsType.Video) {
            R.string.media_details_type_video
        } else {
            R.string.media_details_type_image
        }
    )
    val origin = stringResource(
        if (details.origin == MediaDetailsOrigin.AuthorizedStatus) {
            R.string.media_details_origin_authorized_status
        } else {
            R.string.media_details_origin_saved_estadogo
        }
    )
    val resolution = if (details.widthPixels != null && details.heightPixels != null) {
        stringResource(R.string.media_details_resolution_value, details.widthPixels, details.heightPixels)
    } else {
        unavailable
    }
    val rows = buildList {
        add(stringResource(R.string.media_details_type_label) to type)
        add(stringResource(R.string.media_details_format_label) to (formatter.formatMimeType(details.mimeType) ?: unavailable))
        add(stringResource(R.string.media_details_size_label) to (formatter.formatSize(details.sizeBytes) ?: unavailable))
        add(stringResource(R.string.media_details_date_label) to (formatter.formatDateTime(details.dateTimeMillis) ?: unavailable))
        add(stringResource(R.string.media_details_resolution_label) to resolution)
        if (details.type == MediaDetailsType.Video) {
            add(stringResource(R.string.media_details_duration_label) to (formatter.formatDuration(details.durationMillis) ?: unavailable))
        }
        add(stringResource(R.string.media_details_origin_label) to origin)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { (label, value) ->
            MediaDetailsRow(label = label, value = value)
        }
    }
}

@Composable
private fun MediaDetailsRow(
    label: String,
    value: String
) {
    val colors = LocalGuardaEstadosColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceStrong,
        contentColor = colors.title,
        border = BorderStroke(1.dp, colors.border),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(0.38f),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                modifier = Modifier.weight(0.62f),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
