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
import androidx.compose.material3.RangeSlider
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
import com.guardaestados.R
import com.guardaestados.domain.video.GeneratedVideoPart
import com.guardaestados.domain.video.ReadableVideoDurationFormatter
import com.guardaestados.domain.video.SelectedVideo
import com.guardaestados.domain.video.VideoSplitProgress
import com.guardaestados.domain.video.VideoTrimPlanner
import com.guardaestados.domain.video.VideoTrimRange
import com.guardaestados.domain.video.VideoTrimRangeValidation
import com.guardaestados.ui.theme.LocalGuardaEstadosColors
import com.guardaestados.ui.video.VideoPlayerPreview
import com.guardaestados.ui.video.VideoShareStatus
import com.guardaestados.ui.video.VideoSplitterMessage
import com.guardaestados.ui.video.VideoSplitterMode
import com.guardaestados.ui.video.VideoSplitterStatus
import com.guardaestados.ui.video.VideoSplitterUiState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    onModeSelected: (VideoSplitterMode) -> Unit,
    onPartDurationSelected: (Int) -> Unit,
    onTrimRangeChanged: (Int, Int) -> Unit,
    onAdjustTrimStart: (Int) -> Unit,
    onAdjustTrimEnd: (Int) -> Unit,
    onCreateParts: () -> Unit,
    onCreateTrim: () -> Unit,
    onCancelProcessing: () -> Unit,
    onPreviewOriginal: () -> Unit,
    onPreviewTrimRange: () -> Unit,
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
    val previewGeneratedVideo: (GeneratedVideoPart) -> Unit = { part ->
        onPreviewPart(part)
        coroutineScope.launch {
            previewRequester.bringIntoView()
        }
    }
    val previewSelectedTrimRange: () -> Unit = {
        onPreviewTrimRange()
        coroutineScope.launch {
            previewRequester.bringIntoView()
        }
    }

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
                val previewRange = uiState.previewTrimRange
                val trimFormatter = remember { VideoTrimPlanner() }
                GlassCard(
                    modifier = Modifier.bringIntoViewRequester(previewRequester),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VideoPlayerPreview(
                            uri = previewUri,
                            previewStartMs = previewRange?.startMs,
                            previewStopMs = previewRange?.endMs,
                            playbackRequestKey = uiState.previewRequestKey,
                            autoPlayOnRequest = uiState.previewShouldAutoPlay,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 260.dp, max = 520.dp)
                        )
                        if (previewRange != null) {
                            Text(
                                text = stringResource(
                                    R.string.video_trim_preview_range,
                                    trimFormatter.formatSeconds(previewRange.startSeconds),
                                    trimFormatter.formatSeconds(previewRange.endSeconds)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = SplitBody
                            )
                        }
                    }
                }
            } else {
                VideoEmptyCard()
            }

            uiState.selectedVideo?.let { video ->
                SelectedVideoCard(
                    video = video,
                    activeMode = uiState.activeMode,
                    selectedPartSeconds = uiState.selectedPartSeconds,
                    estimatedParts = uiState.estimatedParts,
                    trimRange = uiState.trimRange,
                    trimRangeValidation = uiState.trimRangeValidation,
                    processing = uiState.status == VideoSplitterStatus.Processing,
                    progress = uiState.progress,
                    onModeSelected = onModeSelected,
                    onPartDurationSelected = onPartDurationSelected,
                    onTrimRangeChanged = onTrimRangeChanged,
                    onAdjustTrimStart = onAdjustTrimStart,
                    onAdjustTrimEnd = onAdjustTrimEnd,
                    onCreateParts = onCreateParts,
                    onCreateTrim = onCreateTrim,
                    onCancelProcessing = onCancelProcessing,
                    onPreviewOriginal = onPreviewOriginal,
                    onPreviewTrimRange = previewSelectedTrimRange
                )
            }

            if (uiState.activeMode == VideoSplitterMode.Split && uiState.generatedParts.isNotEmpty()) {
                GeneratedPartsCard(
                    parts = uiState.generatedParts,
                    sharing = uiState.shareStatus == VideoShareStatus.Opening,
                    onPreviewPart = previewGeneratedVideo,
                    onSharePart = onSharePart,
                    onShareAllParts = onShareAllParts
                )
            }

            val generatedTrim = uiState.generatedTrim
            if (uiState.activeMode == VideoSplitterMode.Trim && generatedTrim != null) {
                GeneratedTrimCard(
                    trim = generatedTrim,
                    sharing = uiState.shareStatus == VideoShareStatus.Opening,
                    onPreviewTrim = previewGeneratedVideo,
                    onShareTrim = onSharePart
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
    activeMode: VideoSplitterMode,
    selectedPartSeconds: Int,
    estimatedParts: Int,
    trimRange: VideoTrimRange,
    trimRangeValidation: VideoTrimRangeValidation,
    processing: Boolean,
    progress: VideoSplitProgress?,
    onModeSelected: (VideoSplitterMode) -> Unit,
    onPartDurationSelected: (Int) -> Unit,
    onTrimRangeChanged: (Int, Int) -> Unit,
    onAdjustTrimStart: (Int) -> Unit,
    onAdjustTrimEnd: (Int) -> Unit,
    onCreateParts: () -> Unit,
    onCreateTrim: () -> Unit,
    onCancelProcessing: () -> Unit,
    onPreviewOriginal: () -> Unit,
    onPreviewTrimRange: () -> Unit
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
            VideoModeSelector(
                activeMode = activeMode,
                processing = processing,
                onModeSelected = onModeSelected
            )
            when (activeMode) {
                VideoSplitterMode.Split -> SplitPartsControls(
                    selectedPartSeconds = selectedPartSeconds,
                    estimatedParts = estimatedParts,
                    processing = processing,
                    progress = progress,
                    onPartDurationSelected = onPartDurationSelected,
                    onCreateParts = onCreateParts,
                    onCancelProcessing = onCancelProcessing
                )
                VideoSplitterMode.Trim -> TrimVideoControls(
                    video = video,
                    trimRange = trimRange,
                    trimRangeValidation = trimRangeValidation,
                    processing = processing,
                    onTrimRangeChanged = onTrimRangeChanged,
                    onAdjustTrimStart = onAdjustTrimStart,
                    onAdjustTrimEnd = onAdjustTrimEnd,
                    onCreateTrim = onCreateTrim,
                    onPreviewTrimRange = onPreviewTrimRange,
                    onCancelProcessing = onCancelProcessing
                )
            }
            when (activeMode) {
                VideoSplitterMode.Split -> SplitSecondaryButton(
                    text = stringResource(R.string.video_splitter_preview_original),
                    onClick = onPreviewOriginal,
                    enabled = !processing,
                    modifier = Modifier.fillMaxWidth()
                )
                VideoSplitterMode.Trim -> Unit
            }
        }
    }
}

@Composable
private fun VideoModeSelector(
    activeMode: VideoSplitterMode,
    processing: Boolean,
    onModeSelected: (VideoSplitterMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.video_splitter_mode_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = SplitText
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VideoFilterChip(
                selected = activeMode == VideoSplitterMode.Split,
                text = stringResource(R.string.video_splitter_mode_split),
                processing = processing,
                onClick = { onModeSelected(VideoSplitterMode.Split) }
            )
            VideoFilterChip(
                selected = activeMode == VideoSplitterMode.Trim,
                text = stringResource(R.string.video_splitter_mode_trim),
                processing = processing,
                onClick = { onModeSelected(VideoSplitterMode.Trim) }
            )
        }
    }
}

@Composable
private fun SplitPartsControls(
    selectedPartSeconds: Int,
    estimatedParts: Int,
    processing: Boolean,
    progress: VideoSplitProgress?,
    onPartDurationSelected: (Int) -> Unit,
    onCreateParts: () -> Unit,
    onCancelProcessing: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                VideoFilterChip(
                    selected = selectedPartSeconds == seconds,
                    text = stringResource(R.string.video_splitter_seconds_option, seconds),
                    processing = processing,
                    onClick = { onPartDurationSelected(seconds) }
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
        SplitPrimaryButton(
            text = stringResource(if (processing) R.string.video_splitter_action_cancel else R.string.video_splitter_action_create),
            onClick = if (processing) onCancelProcessing else onCreateParts,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun VideoFilterChip(
    selected: Boolean,
    text: String,
    processing: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = !processing,
        label = { Text(text = text) },
        border = FilterChipDefaults.filterChipBorder(
            enabled = !processing,
            selected = selected,
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

@Composable
private fun TrimVideoControls(
    video: SelectedVideo,
    trimRange: VideoTrimRange,
    trimRangeValidation: VideoTrimRangeValidation,
    processing: Boolean,
    onTrimRangeChanged: (Int, Int) -> Unit,
    onAdjustTrimStart: (Int) -> Unit,
    onAdjustTrimEnd: (Int) -> Unit,
    onCreateTrim: () -> Unit,
    onPreviewTrimRange: () -> Unit,
    onCancelProcessing: () -> Unit
) {
    val planner = remember { VideoTrimPlanner() }
    val totalSeconds = planner.durationSeconds(video.durationMs)
    val isValid = trimRangeValidation == VideoTrimRangeValidation.Valid
    val canDecreaseStart = !processing && trimRange.startSeconds > 0
    val canIncreaseStart = !processing && trimRange.startSeconds < trimRange.endSeconds - 1
    val canDecreaseEnd = !processing && trimRange.endSeconds > trimRange.startSeconds + 1
    val canIncreaseEnd = !processing && trimRange.endSeconds < totalSeconds
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        VideoDetailRow(label = stringResource(R.string.video_splitter_output_location), value = stringResource(R.string.video_trim_output_path))
        Text(
            text = stringResource(R.string.video_trim_range_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = SplitText
        )
        if (totalSeconds > 0) {
            RangeSlider(
                value = trimRange.startSeconds.toFloat()..trimRange.endSeconds.toFloat(),
                onValueChange = { range ->
                    onTrimRangeChanged(range.start.roundToInt(), range.endInclusive.roundToInt())
                },
                enabled = !processing,
                valueRange = 0f..totalSeconds.toFloat(),
                steps = (totalSeconds - 1).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrimEndpointControl(
                title = stringResource(R.string.video_trim_start_label),
                value = planner.formatSeconds(trimRange.startSeconds),
                onDecrease = { onAdjustTrimStart(-5) },
                onIncrease = { onAdjustTrimStart(5) },
                decreaseEnabled = canDecreaseStart,
                increaseEnabled = canIncreaseStart,
                modifier = Modifier.weight(1f)
            )
            TrimEndpointControl(
                title = stringResource(R.string.video_trim_end_label),
                value = planner.formatSeconds(trimRange.endSeconds),
                onDecrease = { onAdjustTrimEnd(-5) },
                onIncrease = { onAdjustTrimEnd(5) },
                decreaseEnabled = canDecreaseEnd,
                increaseEnabled = canIncreaseEnd,
                modifier = Modifier.weight(1f)
            )
        }
        VideoDetailRow(
            label = stringResource(R.string.video_trim_final_duration),
            value = planner.formatSeconds(planner.durationSeconds(trimRange))
        )
        if (!isValid && !processing) {
            Text(
                text = stringResource(trimRangeValidation.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = SplitBody
            )
        }
        if (processing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = SplitGreen,
                trackColor = SplitBorder
            )
            Text(
                text = stringResource(R.string.video_trim_processing_status),
                style = MaterialTheme.typography.bodyMedium,
                color = SplitBody
            )
            SplitPrimaryButton(
                text = stringResource(R.string.video_splitter_action_cancel),
                onClick = onCancelProcessing,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (isValid) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SplitSecondaryButton(
                    text = stringResource(R.string.video_trim_preview),
                    onClick = onPreviewTrimRange,
                    modifier = Modifier.weight(1f)
                )
                SplitPrimaryButton(
                    text = stringResource(R.string.video_trim_action_create),
                    onClick = onCreateTrim,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TrimEndpointControl(
    title: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseEnabled: Boolean,
    increaseEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SplitSurfaceStrong)
            .border(1.dp, SplitBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = SplitBody
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = SplitText
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TrimAdjustButton(
                text = stringResource(R.string.video_trim_adjust_minus_5),
                onClick = onDecrease,
                enabled = decreaseEnabled,
                modifier = Modifier.weight(1f)
            )
            TrimAdjustButton(
                text = stringResource(R.string.video_trim_adjust_plus_5),
                onClick = onIncrease,
                enabled = increaseEnabled,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TrimAdjustButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SplitSurface.copy(alpha = if (enabled) 1f else 0.54f))
            .border(1.dp, SplitBorder, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) SplitBody else SplitBody.copy(alpha = 0.58f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
private fun GeneratedTrimCard(
    trim: GeneratedVideoPart,
    sharing: Boolean,
    onPreviewTrim: (GeneratedVideoPart) -> Unit,
    onShareTrim: (GeneratedVideoPart) -> Unit
) {
    val formatter = ReadableVideoDurationFormatter()
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.video_trim_generated_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SplitText
            )
            Text(
                text = stringResource(R.string.video_splitter_share_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = SplitBody
            )
            Text(
                text = stringResource(R.string.video_splitter_part_duration, formatter.format(trim.durationMs)),
                style = MaterialTheme.typography.bodyMedium,
                color = SplitBody
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SplitSecondaryButton(
                    text = stringResource(R.string.video_trim_preview),
                    onClick = { onPreviewTrim(trim) },
                    modifier = Modifier.weight(1f)
                )
                SplitSecondaryButton(
                    text = stringResource(R.string.video_trim_share),
                    onClick = { onShareTrim(trim) },
                    enabled = !sharing,
                    modifier = Modifier.weight(1f),
                    accent = true
                )
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
private fun VideoTrimRangeValidation.messageRes(): Int {
    return when (this) {
        VideoTrimRangeValidation.Valid -> R.string.video_trim_range_valid
        VideoTrimRangeValidation.UnknownDuration -> R.string.video_trim_range_unknown_duration
        VideoTrimRangeValidation.StartNotBeforeEnd -> R.string.video_trim_range_invalid_order
        VideoTrimRangeValidation.TooShort -> R.string.video_trim_range_too_short
        VideoTrimRangeValidation.OutsideDuration -> R.string.video_trim_range_outside_duration
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
        VideoSplitterMessage.InvalidTrimRange -> R.string.video_trim_range_invalid
        VideoSplitterMessage.DestinationPermissionLost -> R.string.video_trim_destination_permission_lost
        VideoSplitterMessage.DestinationUnavailable -> R.string.video_trim_destination_unavailable
        VideoSplitterMessage.Cancelled -> R.string.video_splitter_cancelled
        VideoSplitterMessage.SplitSuccess -> R.string.video_splitter_success
        VideoSplitterMessage.TrimSuccess -> R.string.video_trim_success
        VideoSplitterMessage.ShareChooserOpened -> R.string.video_splitter_share_opened
        VideoSplitterMessage.ShareNoCompatibleApp -> R.string.video_splitter_share_no_app
        VideoSplitterMessage.ShareError -> R.string.video_splitter_share_error
    }
}
