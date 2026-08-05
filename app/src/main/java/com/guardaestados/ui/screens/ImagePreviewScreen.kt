package com.guardaestados.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.guardaestados.R
import com.guardaestados.domain.status.StatusImage
import com.guardaestados.domain.status.StatusMediaType
import com.guardaestados.ui.save.SaveStatusImageUiState
import com.guardaestados.ui.share.ShareStatusImageUiState
import com.guardaestados.ui.status.StatusImagePreviewState
import com.guardaestados.ui.video.VideoPlayerPreview
import java.text.DateFormat
import java.util.Date
private val PreviewBackground = Color(0xFF030A1C)
private val PreviewSurface = Color(0xFF101A30)
private val PreviewSurfaceHigh = Color(0xFF15213A)
private val PreviewText = Color(0xFFF4F7FF)
private val PreviewSecondaryText = Color(0xFFAAB5CE)
private val PreviewBorder = Color(0xFF263451)
private val PreviewGreen = Color(0xFF24D18B)
private val PreviewViolet = Color(0xFF7C5CFF)
private val PreviewFuchsia = Color(0xFFFF4FD8)
private val PreviewGradient = Brush.horizontalGradient(listOf(PreviewGreen, PreviewViolet, PreviewFuchsia))

@Composable
fun ImagePreviewScreen(
    previewState: StatusImagePreviewState,
    saveState: SaveStatusImageUiState,
    shareState: ShareStatusImageUiState,
    onSaveImage: (StatusImage) -> Unit,
    onShareImage: (StatusImage) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = PreviewBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PreviewTopBar(onBack = onBack)

            when (previewState) {
                StatusImagePreviewState.Loading -> PreviewMessageCard(
                    title = stringResource(R.string.preview_loading_title),
                    body = stringResource(R.string.preview_loading_body)
                )

                StatusImagePreviewState.NoFolderSelected -> PreviewMessageCard(
                    title = stringResource(R.string.states_no_folder_title),
                    body = stringResource(R.string.states_no_folder_body)
                )

                StatusImagePreviewState.PermissionLost -> PreviewMessageCard(
                    title = stringResource(R.string.states_permission_lost_title),
                    body = stringResource(R.string.states_permission_lost_body)
                )

                StatusImagePreviewState.Unavailable -> PreviewMessageCard(
                    title = stringResource(R.string.preview_unavailable_title),
                    body = stringResource(R.string.preview_unavailable_body)
                )

                is StatusImagePreviewState.Content -> PreviewContent(
                    image = previewState.image,
                    saveState = saveState,
                    shareState = shareState,
                    onSaveImage = onSaveImage,
                    onShareImage = onShareImage
                )
            }
        }
    }
}

@Composable
private fun PreviewTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = PreviewSurface.copy(alpha = 0.72f),
            contentColor = PreviewText,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, PreviewBorder)
        ) {
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.preview_action_back), color = PreviewText)
            }
        }
    }
}

@Composable
private fun PreviewContent(
    image: StatusImage,
    saveState: SaveStatusImageUiState,
    shareState: ShareStatusImageUiState,
    onSaveImage: (StatusImage) -> Unit,
    onShareImage: (StatusImage) -> Unit
) {
    var failedToLoad by remember(image.uri) { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 360.dp, max = 650.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (image.mediaType == StatusMediaType.Video) {
                VideoPlayerPreview(
                    uri = image.uri,
                    modifier = Modifier.fillMaxSize(),
                    showPlaybackStatus = true,
                    errorMessage = stringResource(R.string.preview_video_load_error_body)
                )
            } else if (failedToLoad) {
                Text(
                    text = stringResource(R.string.preview_load_error_body),
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = PreviewSecondaryText
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(image.uri)
                        .build(),
                    contentDescription = stringResource(R.string.preview_image_content_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    onError = { failedToLoad = true }
                )
            }
        }

        MediaInfoCard(image = image)

        PreviewActionsCard(
            image = image,
            saveState = saveState,
            shareState = shareState,
            onSaveImage = onSaveImage,
            onShareImage = onShareImage
        )
    }
}

@Composable
private fun MediaInfoCard(image: StatusImage) {
    val formattedDate = image.lastModifiedMillis?.formatDate()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PreviewSurface),
        border = BorderStroke(1.dp, PreviewBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(if (image.mediaType == StatusMediaType.Video) R.string.preview_video_status_label else R.string.preview_image_status_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = PreviewText
            )
            formattedDate?.let { date ->
                Text(
                    text = stringResource(R.string.preview_media_date, date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PreviewSecondaryText
                )
            }
        }
    }
}

@Composable
private fun PreviewActionsCard(
    image: StatusImage,
    saveState: SaveStatusImageUiState,
    shareState: ShareStatusImageUiState,
    onSaveImage: (StatusImage) -> Unit,
    onShareImage: (StatusImage) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = PreviewSurfaceHigh),
        border = BorderStroke(1.dp, PreviewBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GradientActionButton(
                    text = stringResource(R.string.preview_action_save_short),
                    iconRes = R.drawable.ic_nav_saved,
                    onClick = { onSaveImage(image) },
                    enabled = saveState != SaveStatusImageUiState.Saving,
                    modifier = Modifier.weight(1f)
                )

                SecondaryActionButton(
                    text = stringResource(R.string.preview_action_share_short),
                    iconRes = R.drawable.ic_share,
                    onClick = { onShareImage(image) },
                    enabled = shareState != ShareStatusImageUiState.Sharing,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SaveStatusMessage(saveState = saveState)
                ShareStatusMessage(shareState = shareState)
            }
        }
    }
}

@Composable
private fun GradientActionButton(
    text: String,
    iconRes: Int,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = PreviewText,
            disabledContainerColor = PreviewBorder,
            disabledContentColor = PreviewSecondaryText
        ),
        contentPadding = ButtonDefaults.ContentPadding,
        shape = RoundedCornerShape(10.dp)
    ) {
        ActionButtonContent(
            text = text,
            iconRes = iconRes,
            background = if (enabled) PreviewGradient else Brush.horizontalGradient(listOf(PreviewBorder, PreviewBorder))
        )
    }
}

@Composable
private fun SecondaryActionButton(
    text: String,
    iconRes: Int,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PreviewSurface,
            contentColor = PreviewText,
            disabledContainerColor = PreviewSurface.copy(alpha = 0.54f),
            disabledContentColor = PreviewSecondaryText
        ),
        border = BorderStroke(1.dp, PreviewBorder),
        contentPadding = ButtonDefaults.ContentPadding,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = text,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ActionButtonContent(
    text: String,
    iconRes: Int,
    background: Brush
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = text,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SaveStatusMessage(saveState: SaveStatusImageUiState) {
    StatusText(
        message = when (saveState) {
            SaveStatusImageUiState.Idle -> null
            SaveStatusImageUiState.Saving -> stringResource(R.string.save_status_saving)
            SaveStatusImageUiState.Duplicate -> stringResource(R.string.save_status_duplicate_video)
            SaveStatusImageUiState.DestinationPermissionLost -> stringResource(R.string.save_status_destination_permission_lost)
            SaveStatusImageUiState.DestinationUnavailable -> stringResource(R.string.save_status_destination_error)
            SaveStatusImageUiState.Error -> stringResource(R.string.save_status_error)
            is SaveStatusImageUiState.Success -> stringResource(R.string.save_status_success, saveState.displayName)
        },
        isError = saveState == SaveStatusImageUiState.Error ||
            saveState == SaveStatusImageUiState.DestinationPermissionLost ||
            saveState == SaveStatusImageUiState.DestinationUnavailable
    )
}

@Composable
private fun ShareStatusMessage(shareState: ShareStatusImageUiState) {
    StatusText(
        message = when (shareState) {
            ShareStatusImageUiState.Idle -> null
            ShareStatusImageUiState.Sharing -> stringResource(R.string.share_status_opening)
            ShareStatusImageUiState.ChooserOpened -> stringResource(R.string.share_status_chooser_opened)
            ShareStatusImageUiState.NoCompatibleApp -> stringResource(R.string.share_status_no_app)
            ShareStatusImageUiState.Error -> stringResource(R.string.share_status_error)
        },
        isError = shareState == ShareStatusImageUiState.Error || shareState == ShareStatusImageUiState.NoCompatibleApp
    )
}

@Composable
private fun StatusText(
    message: String?,
    isError: Boolean
) {
    AnimatedVisibility(visible = message != null) {
        Text(
            text = message.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) Color(0xFFFFB4AB) else PreviewSecondaryText
        )
    }
}

@Composable
private fun PreviewMessageCard(
    title: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PreviewSurface),
        border = BorderStroke(1.dp, PreviewBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = PreviewText
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = PreviewSecondaryText
            )
        }
    }
}

private fun Long.formatDate(): String {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(this))
}