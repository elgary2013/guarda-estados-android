package com.guardaestados.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.guardaestados.domain.saved.SavedImage
import com.guardaestados.domain.saved.SavedMediaOrigin
import com.guardaestados.domain.saved.SavedMediaType
import com.guardaestados.ui.saved.SavedImageDeleteState
import com.guardaestados.ui.saved.SavedImageOpenState
import com.guardaestados.ui.saved.SavedImagePreviewState
import com.guardaestados.ui.saved.SavedImageShareState
import com.guardaestados.ui.status.StatusImagePresentationFormatter
import com.guardaestados.ui.video.VideoPlayerPreview
import java.text.DateFormat
import java.util.Date

@Composable
fun SavedImagePreviewScreen(
    previewState: SavedImagePreviewState,
    deleteState: SavedImageDeleteState,
    shareState: SavedImageShareState,
    openState: SavedImageOpenState,
    onDeleteImage: (SavedImage) -> Unit,
    onShareImage: (SavedImage) -> Unit,
    onOpenImage: (SavedImage) -> Unit,
    onShareMessageDismissed: () -> Unit,
    onOpenMessageDismissed: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(deleteState) {
        if (deleteState == SavedImageDeleteState.Success || deleteState == SavedImageDeleteState.AlreadyMissing) {
            onBack()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SavedPreviewTopBar(onBack = onBack)

            deleteState.statusMessageRes()?.let { messageRes ->
                SavedPreviewMessageCard(
                    title = stringResource(R.string.saved_delete_dialog_title),
                    body = stringResource(messageRes)
                )
            }

            shareState.statusMessageRes()?.let { messageRes ->
                SavedPreviewStatusCard(
                    message = stringResource(messageRes),
                    canDismiss = shareState.canDismiss(),
                    onDismiss = onShareMessageDismissed
                )
            }

            openState.statusMessageRes()?.let { messageRes ->
                SavedPreviewStatusCard(
                    message = stringResource(messageRes),
                    canDismiss = openState.canDismiss(),
                    onDismiss = onOpenMessageDismissed
                )
            }

            when (previewState) {
                SavedImagePreviewState.Loading -> SavedPreviewMessageCard(
                    title = stringResource(R.string.saved_loading_title),
                    body = stringResource(R.string.saved_loading_body)
                )

                SavedImagePreviewState.Unavailable -> SavedPreviewMessageCard(
                    title = stringResource(R.string.saved_preview_unavailable_title),
                    body = stringResource(R.string.saved_preview_unavailable_body)
                )

                is SavedImagePreviewState.Content -> SavedPreviewContent(
                    image = previewState.image,
                    deleteState = deleteState,
                    shareState = shareState,
                    openState = openState,
                    onDeleteImage = onDeleteImage,
                    onShareImage = onShareImage,
                    onOpenImage = onOpenImage
                )
            }
        }
    }
}

@Composable
private fun SavedPreviewTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) {
            Text(text = stringResource(R.string.preview_action_back))
        }
    }
}

@Composable
private fun SavedPreviewContent(
    image: SavedImage,
    deleteState: SavedImageDeleteState,
    shareState: SavedImageShareState,
    openState: SavedImageOpenState,
    onDeleteImage: (SavedImage) -> Unit,
    onShareImage: (SavedImage) -> Unit,
    onOpenImage: (SavedImage) -> Unit
) {
    var failedToLoad by remember(image.uri) { mutableStateOf(false) }
    var showDeleteDialog by remember(image.uri) { mutableStateOf(false) }
    var showDetails by remember(image.uri) { mutableStateOf(false) }
    val deleting = deleteState == SavedImageDeleteState.Deleting ||
        deleteState is SavedImageDeleteState.NeedsSystemConfirmation
    val sharing = shareState == SavedImageShareState.Sharing
    val opening = openState == SavedImageOpenState.Opening
    val busy = deleting || sharing || opening

    if (showDeleteDialog) {
        ConfirmSavedImageDeleteDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDeleteImage(image)
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 320.dp, max = 620.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (image.mediaType == SavedMediaType.Video) {
                    VideoPlayerPreview(
                        uri = image.uri,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (failedToLoad) {
                    Text(
                        text = stringResource(R.string.preview_load_error_body),
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(image.uri)
                            .build(),
                        contentDescription = stringResource(R.string.saved_image_card_description),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        onError = { failedToLoad = true }
                    )
                }
            }

            DetailsToggleButton(
                expanded = showDetails,
                onClick = { showDetails = !showDetails }
            )

            AnimatedVisibility(visible = showDetails) {
                SavedImageDetailsCard(image = image)
            }

            SavedPreviewActions(
                busy = busy,
                onShareImage = { onShareImage(image) },
                onOpenImage = { onOpenImage(image) },
                onDeleteImage = { showDeleteDialog = true }
            )
        }
    }
}

@Composable
private fun DetailsToggleButton(
    expanded: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(if (expanded) R.string.saved_action_hide_details else R.string.saved_action_view_details),
            maxLines = 1
        )
    }
}

@Composable
private fun SavedPreviewActions(
    busy: Boolean,
    onShareImage: () -> Unit,
    onOpenImage: () -> Unit,
    onDeleteImage: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onShareImage,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_share),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(R.string.saved_action_share_media),
                maxLines = 1
            )
        }

        OutlinedButton(
            onClick = onOpenImage,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
        ) {
            Text(text = stringResource(R.string.saved_action_open_with), maxLines = 1)
        }

        Button(
            onClick = onDeleteImage,
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(R.string.saved_action_delete),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SavedImageDetailsCard(image: SavedImage) {
    val unavailable = stringResource(R.string.status_image_value_unavailable)
    val formattedDate = image.dateAddedMillis?.formatDate()
    val formatter = remember { StatusImagePresentationFormatter() }
    val size = formatter.sizeValue(image.sizeBytes) ?: unavailable

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailRow(
                label = stringResource(R.string.saved_detail_origin_label),
                value = stringResource(image.origin.labelRes())
            )
            DetailRow(
                label = stringResource(R.string.saved_detail_type_label),
                value = image.mimeType
            )
            DetailRow(
                label = stringResource(R.string.saved_detail_size_label),
                value = size
            )
            DetailRow(
                label = stringResource(R.string.saved_detail_saved_date_label),
                value = formattedDate ?: unavailable
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.38f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.62f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SavedPreviewMessageCard(
    title: String,
    body: String
) {
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SavedPreviewStatusCard(
    message: String,
    canDismiss: Boolean,
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
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            if (canDismiss) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.saved_delete_dialog_cancel))
                }
            }
        }
    }
}

private fun SavedImageDeleteState.statusMessageRes(): Int? {
    return when (this) {
        SavedImageDeleteState.Idle -> null
        SavedImageDeleteState.Deleting -> R.string.saved_delete_status_deleting
        SavedImageDeleteState.Success -> R.string.saved_delete_status_success
        SavedImageDeleteState.AlreadyMissing -> R.string.saved_delete_status_missing
        SavedImageDeleteState.InvalidTarget -> R.string.saved_delete_status_invalid
        SavedImageDeleteState.Error -> R.string.saved_delete_status_error
        is SavedImageDeleteState.NeedsSystemConfirmation -> R.string.saved_delete_system_confirmation
    }
}

private fun SavedImageShareState.statusMessageRes(): Int? {
    return when (this) {
        SavedImageShareState.Idle -> null
        SavedImageShareState.Sharing -> R.string.saved_share_status_opening
        SavedImageShareState.ChooserOpened -> R.string.saved_share_status_chooser_opened
        SavedImageShareState.AlreadyMissing -> R.string.saved_share_status_missing
        SavedImageShareState.InvalidTarget -> R.string.saved_share_status_invalid
        SavedImageShareState.NoCompatibleApp -> R.string.saved_share_status_no_app
        SavedImageShareState.Error -> R.string.saved_share_status_error
    }
}

private fun SavedImageShareState.canDismiss(): Boolean {
    return this != SavedImageShareState.Sharing
}

private fun SavedImageOpenState.statusMessageRes(): Int? {
    return when (this) {
        SavedImageOpenState.Idle -> null
        SavedImageOpenState.Opening -> R.string.saved_open_status_opening
        SavedImageOpenState.ViewerOpened -> R.string.saved_open_status_viewer_opened
        SavedImageOpenState.AlreadyMissing -> R.string.saved_share_status_missing
        SavedImageOpenState.InvalidTarget -> R.string.saved_share_status_invalid
        SavedImageOpenState.NoCompatibleApp -> R.string.saved_open_status_no_app
        SavedImageOpenState.Error -> R.string.saved_open_status_error
    }
}

private fun SavedImageOpenState.canDismiss(): Boolean {
    return this != SavedImageOpenState.Opening
}

private fun SavedMediaOrigin.labelRes(): Int {
    return when (this) {
        SavedMediaOrigin.SavedStatus -> R.string.saved_origin_status_copy
        SavedMediaOrigin.VideoPart -> R.string.saved_origin_video_part
    }
}

private fun Long.formatDate(): String {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(this))
}