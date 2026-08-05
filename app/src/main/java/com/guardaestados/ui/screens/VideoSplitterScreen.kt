package com.guardaestados.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guardaestados.R
import com.guardaestados.domain.video.GeneratedVideoPart
import com.guardaestados.domain.video.ReadableVideoDurationFormatter
import com.guardaestados.domain.video.SelectedVideo
import com.guardaestados.domain.video.VideoSplitProgress
import com.guardaestados.ui.video.VideoPlayerPreview
import com.guardaestados.ui.video.VideoShareStatus
import com.guardaestados.ui.video.VideoSplitterMessage
import com.guardaestados.ui.video.VideoSplitterStatus
import com.guardaestados.ui.video.VideoSplitterUiState
import com.guardaestados.ui.theme.BrandGradientButton
import com.guardaestados.ui.theme.brandGradientBorder

@Composable
fun VideoSplitterScreen(
    uiState: VideoSplitterUiState,
    onPickVideo: () -> Unit,
    onPartDurationSelected: (Int) -> Unit,
    onCreateParts: () -> Unit,
    onCancelProcessing: () -> Unit,
    onPreviewOriginal: () -> Unit,
    onPreviewPart: (GeneratedVideoPart) -> Unit,
    onSharePart: (GeneratedVideoPart) -> Unit,
    onShareAllParts: () -> Unit,
    onClearMessage: () -> Unit,
    onBack: () -> Unit,
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
            Button(onClick = onBack) {
                Text(text = stringResource(R.string.preview_action_back))
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.video_splitter_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.video_splitter_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            VideoNoticeCard()

            uiState.message?.let { message ->
                VideoStatusCard(
                    messageRes = message.stringRes(),
                    onDismiss = onClearMessage
                )
            }

            BrandGradientButton(
                text = stringResource(R.string.video_splitter_action_pick),
                onClick = onPickVideo,
                enabled = uiState.status != VideoSplitterStatus.Processing,
                modifier = Modifier.fillMaxWidth(),
                highlight = true
            )

            val previewUri = uiState.previewUri
            if (previewUri != null) {
                VideoPlayerPreview(
                    uri = previewUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp, max = 520.dp)
                )
            } else {
                VideoEmptyCard()
            }

            uiState.selectedVideo?.let { video ->
                SelectedVideoCard(
                    video = video,
                    selectedPartSeconds = uiState.selectedPartSeconds,
                    estimatedParts = uiState.estimatedParts,
                    processing = uiState.status == VideoSplitterStatus.Processing,
                    progress = uiState.progress,
                    onPartDurationSelected = onPartDurationSelected,
                    onCreateParts = onCreateParts,
                    onCancelProcessing = onCancelProcessing,
                    onPreviewOriginal = onPreviewOriginal
                )
            }

            if (uiState.generatedParts.isNotEmpty()) {
                GeneratedPartsCard(
                    parts = uiState.generatedParts,
                    sharing = uiState.shareStatus == VideoShareStatus.Opening,
                    onPreviewPart = onPreviewPart,
                    onSharePart = onSharePart,
                    onShareAllParts = onShareAllParts
                )
            }
        }
    }
}

@Composable
private fun VideoNoticeCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = stringResource(R.string.video_splitter_privacy_notice),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun VideoEmptyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.video_splitter_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.video_splitter_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SelectedVideoCard(
    video: SelectedVideo,
    selectedPartSeconds: Int,
    estimatedParts: Int,
    processing: Boolean,
    progress: VideoSplitProgress?,
    onPartDurationSelected: (Int) -> Unit,
    onCreateParts: () -> Unit,
    onCancelProcessing: () -> Unit,
    onPreviewOriginal: () -> Unit
) {
    val formatter = ReadableVideoDurationFormatter()
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
                text = stringResource(R.string.video_splitter_selected_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            VideoDetailRow(
                label = stringResource(R.string.video_splitter_file_name),
                value = video.displayName
            )
            VideoDetailRow(
                label = stringResource(R.string.video_splitter_total_duration),
                value = formatter.format(video.durationMs)
            )
            VideoDetailRow(
                label = stringResource(R.string.video_splitter_estimated_parts),
                value = estimatedParts.toString()
            )
            VideoDetailRow(
                label = stringResource(R.string.video_splitter_output_location),
                value = stringResource(R.string.video_splitter_output_path)
            )

            Text(
                text = stringResource(R.string.video_splitter_part_duration_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 60).forEach { seconds ->
                    FilterChip(
                        selected = selectedPartSeconds == seconds,
                        onClick = { onPartDurationSelected(seconds) },
                        enabled = !processing,
                        label = { Text(text = stringResource(R.string.video_splitter_seconds_option, seconds)) }
                    )
                }
            }

            if (processing) {
                val current = progress?.currentPart ?: 0
                val total = progress?.totalParts ?: estimatedParts
                LinearProgressIndicator(
                    progress = { progress?.fraction ?: 0f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.video_splitter_processing_status, current, total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPreviewOriginal,
                    enabled = !processing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.video_splitter_preview_original))
                }
                BrandGradientButton(
                    text = stringResource(if (processing) R.string.video_splitter_action_cancel else R.string.video_splitter_action_create),
                    onClick = if (processing) onCancelProcessing else onCreateParts,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GeneratedPartsCard(
    parts: List<GeneratedVideoPart>,
    sharing: Boolean,
    onPreviewPart: (GeneratedVideoPart) -> Unit,
    onSharePart: (GeneratedVideoPart) -> Unit,
    onShareAllParts: () -> Unit
) {
    val formatter = ReadableVideoDurationFormatter()
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
                text = stringResource(R.string.video_splitter_generated_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.video_splitter_share_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BrandGradientButton(
                text = stringResource(R.string.video_splitter_share_all),
                onClick = onShareAllParts,
                enabled = !sharing,
                modifier = Modifier.fillMaxWidth(),
                highlight = true
            )
            parts.sortedBy { it.index }.forEach { part ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.video_splitter_part_title, part.index),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = part.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(
                                R.string.video_splitter_part_duration,
                                formatter.format(part.durationMs)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onPreviewPart(part) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = stringResource(R.string.video_splitter_preview_part))
                            }
                            Button(
                                onClick = { onSharePart(part) },
                                enabled = !sharing,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = stringResource(R.string.video_splitter_share_part))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoStatusCard(
    @StringRes messageRes: Int,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.settings_reset_success_dismiss))
            }
        }
    }
}

@Composable
private fun VideoDetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@StringRes
private fun VideoSplitterMessage.stringRes(): Int {
    return when (this) {
        VideoSplitterMessage.UnknownDuration -> R.string.video_splitter_error_unknown_duration
        VideoSplitterMessage.FileUnavailable -> R.string.video_splitter_error_file_unavailable
        VideoSplitterMessage.LoadError -> R.string.video_splitter_error_load
        VideoSplitterMessage.InsufficientStorage -> R.string.video_splitter_error_storage
        VideoSplitterMessage.UnsupportedAndroidVersion -> R.string.video_splitter_error_unsupported_android
        VideoSplitterMessage.ExportError -> R.string.video_splitter_error_export
        VideoSplitterMessage.Cancelled -> R.string.video_splitter_cancelled
        VideoSplitterMessage.SplitSuccess -> R.string.video_splitter_success
        VideoSplitterMessage.ShareChooserOpened -> R.string.video_splitter_share_opened
        VideoSplitterMessage.ShareNoCompatibleApp -> R.string.video_splitter_share_no_app
        VideoSplitterMessage.ShareError -> R.string.video_splitter_share_error
    }
}
