package com.guardaestados.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guardaestados.ui.theme.LocalGuardaEstadosColors
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
import kotlinx.coroutines.launch

private val SplitBackground: Color
    @Composable get() = LocalGuardaEstadosColors.current.background
private val SplitSurface: Color
    @Composable get() = LocalGuardaEstadosColors.current.surface
private val SplitSurfaceStrong: Color
    @Composable get() = LocalGuardaEstadosColors.current.surfaceStrong
private val SplitGreenSoft: Color
    @Composable get() = LocalGuardaEstadosColors.current.surfaceSoft
private val SplitText: Color
    @Composable get() = LocalGuardaEstadosColors.current.title
private val SplitBody: Color
    @Composable get() = LocalGuardaEstadosColors.current.body
private val SplitBorder: Color
    @Composable get() = LocalGuardaEstadosColors.current.border
private val SplitGreen: Color
    @Composable get() = LocalGuardaEstadosColors.current.active
private val SplitGradient: Brush
    @Composable get() = LocalGuardaEstadosColors.current.primaryGradient

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
    val scrollState = rememberScrollState()
    val previewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = SplitBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            VideoSplitterHeader(onBack = onBack)

            VideoNoticeCard()

            uiState.message?.let { message ->
                VideoStatusCard(
                    messageRes = message.stringRes(),
                    onDismiss = onClearMessage
                )
            }

            SplitPrimaryButton(
                text = stringResource(R.string.video_splitter_action_pick),
                onClick = onPickVideo,
                enabled = uiState.status != VideoSplitterStatus.Processing,
                modifier = Modifier.fillMaxWidth()
            )

            val previewUri = uiState.previewUri
            if (previewUri != null) {
                GlassCard(
                    modifier = Modifier.bringIntoViewRequester(previewRequester),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    VideoPlayerPreview(
                        uri = previewUri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 260.dp, max = 520.dp)
                    )
                }
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
                    onPreviewPart = { part ->
                        onPreviewPart(part)
                        coroutineScope.launch {
                            previewRequester.bringIntoView()
                        }
                    },
                    onSharePart = onSharePart,
                    onShareAllParts = onShareAllParts
                )
            }
        }
    }
}

@Composable
private fun VideoSplitterHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.preview_action_back),
                modifier = Modifier.size(24.dp),
                tint = SplitText
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.video_splitter_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = SplitText
            )
            Text(
                text = stringResource(R.string.video_splitter_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = SplitBody
            )
        }
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SplitSurface),
        border = BorderStroke(1.dp, SplitBorder),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
private fun VideoNoticeCard() {
    GlassCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        Text(
            text = stringResource(R.string.video_splitter_privacy_notice),
            style = MaterialTheme.typography.bodySmall,
            color = SplitBody
        )
    }
}

@Composable
private fun VideoEmptyCard() {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.video_splitter_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SplitText
            )
            Text(
                text = stringResource(R.string.video_splitter_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = SplitBody
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
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.video_splitter_selected_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SplitText
            )
            VideoDetailRow(label = stringResource(R.string.video_splitter_total_duration), value = formatter.format(video.durationMs))
            VideoDetailRow(label = stringResource(R.string.video_splitter_estimated_parts), value = estimatedParts.toString())
            VideoDetailRow(label = stringResource(R.string.video_splitter_output_location), value = stringResource(R.string.video_splitter_output_path))

            Text(
                text = stringResource(R.string.video_splitter_part_duration_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = SplitText
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 60).forEach { seconds ->
                    FilterChip(
                        selected = selectedPartSeconds == seconds,
                        onClick = { onPartDurationSelected(seconds) },
                        enabled = !processing,
                        label = { Text(text = stringResource(R.string.video_splitter_seconds_option, seconds)) },
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = !processing,
                            selected = selectedPartSeconds == seconds,
                            borderColor = SplitBorder,
                            selectedBorderColor = SplitGreen
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SplitSurfaceStrong,
                            labelColor = SplitBody,
                            iconColor = SplitBody,
                            selectedContainerColor = SplitGreenSoft,
                            selectedLabelColor = SplitGreen,
                            selectedLeadingIconColor = SplitGreen,
                            disabledContainerColor = SplitSurfaceStrong.copy(alpha = 0.52f),
                            disabledLabelColor = SplitBody.copy(alpha = 0.58f)
                        )
                    )
                }
            }

            if (processing) {
                val current = progress?.currentPart ?: 0
                val total = progress?.totalParts ?: estimatedParts
                LinearProgressIndicator(
                    progress = { progress?.fraction ?: 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = SplitGreen,
                    trackColor = SplitBorder
                )
                Text(
                    text = stringResource(R.string.video_splitter_processing_status, current, total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SplitBody
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SplitSecondaryButton(
                    text = stringResource(R.string.video_splitter_preview_original),
                    onClick = onPreviewOriginal,
                    enabled = !processing,
                    modifier = Modifier.weight(1f)
                )
                SplitPrimaryButton(
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
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.video_splitter_generated_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SplitText
            )
            Text(
                text = stringResource(R.string.video_splitter_share_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = SplitBody
            )
            SplitPrimaryButton(
                text = stringResource(R.string.video_splitter_share_all),
                onClick = onShareAllParts,
                enabled = !sharing,
                modifier = Modifier.fillMaxWidth()
            )
            val sortedParts = parts.sortedBy { it.index }
            val totalParts = sortedParts.size
            sortedParts.forEach { part ->
                GlassCard(contentPadding = PaddingValues(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.video_splitter_part_title, part.index, totalParts),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SplitText
                        )
                        Text(
                            text = stringResource(R.string.video_splitter_part_duration, formatter.format(part.durationMs)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SplitBody
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SplitSecondaryButton(
                                text = stringResource(R.string.video_splitter_preview_part),
                                onClick = { onPreviewPart(part) },
                                modifier = Modifier.weight(1f)
                            )
                            SplitSecondaryButton(
                                text = stringResource(R.string.video_splitter_share_part),
                                onClick = { onSharePart(part) },
                                enabled = !sharing,
                                modifier = Modifier.weight(1f),
                                accent = true
                            )
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
    GlassCard(contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = SplitBody
            )
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.settings_reset_success_dismiss),
                    color = SplitGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SplitPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) SplitGradient else Brush.horizontalGradient(listOf(SplitBorder, SplitBorder)))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = SplitText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SplitSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Boolean = false
) {
    val borderColor = if (accent) SplitGreen.copy(alpha = 0.72f) else SplitBorder
    val contentColor = if (accent) SplitGreen else SplitBody
    Row(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SplitSurfaceStrong.copy(alpha = if (enabled) 1f else 0.54f))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) contentColor else SplitBody.copy(alpha = 0.58f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VideoDetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = SplitBody
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = SplitText
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
