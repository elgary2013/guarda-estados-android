package com.guardaestados.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.guardaestados.R
import com.guardaestados.domain.status.StatusImage
import com.guardaestados.ui.save.SaveStatusImageUiState
import com.guardaestados.ui.share.ShareStatusImageUiState
import com.guardaestados.ui.status.StatusImagePresentationFormatter
import com.guardaestados.ui.status.StatusImagePreviewState
import java.text.DateFormat
import java.util.Date

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

            Text(
                text = stringResource(R.string.preview_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

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
private fun PreviewContent(
    image: StatusImage,
    saveState: SaveStatusImageUiState,
    shareState: ShareStatusImageUiState,
    onSaveImage: (StatusImage) -> Unit,
    onShareImage: (StatusImage) -> Unit
) {
    var failedToLoad by remember(image.uri) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp, max = 560.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (failedToLoad) {
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
                        contentDescription = stringResource(
                            R.string.preview_image_description,
                            image.displayTitle()
                        ),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        onError = { failedToLoad = true }
                    )
                }
            }

            Button(
                onClick = { onSaveImage(image) },
                enabled = saveState != SaveStatusImageUiState.Saving
            ) {
                Text(text = stringResource(R.string.preview_action_save_image))
            }

            Button(
                onClick = { onShareImage(image) },
                enabled = shareState != ShareStatusImageUiState.Sharing
            ) {
                Text(text = stringResource(R.string.preview_action_share_image))
            }

            SaveStatusMessage(saveState = saveState)
            ShareStatusMessage(shareState = shareState)
            ImageInfoCard(image = image)
        }
    }
}

@Composable
private fun SaveStatusMessage(saveState: SaveStatusImageUiState) {
    when (saveState) {
        SaveStatusImageUiState.Idle -> Unit
        SaveStatusImageUiState.Saving -> Text(
            text = stringResource(R.string.save_status_saving),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SaveStatusImageUiState.Error -> Text(
            text = stringResource(R.string.save_status_error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )

        is SaveStatusImageUiState.Success -> Text(
            text = stringResource(R.string.save_status_success, saveState.displayName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ShareStatusMessage(shareState: ShareStatusImageUiState) {
    when (shareState) {
        ShareStatusImageUiState.Idle -> Unit
        ShareStatusImageUiState.Sharing -> Text(
            text = stringResource(R.string.share_status_opening),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ShareStatusImageUiState.ChooserOpened -> Text(
            text = stringResource(R.string.share_status_chooser_opened),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        ShareStatusImageUiState.NoCompatibleApp -> Text(
            text = stringResource(R.string.share_status_no_app),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )

        ShareStatusImageUiState.Error -> Text(
            text = stringResource(R.string.share_status_error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}
@Composable
private fun ImageInfoCard(image: StatusImage) {
    val unavailable = stringResource(R.string.status_image_value_unavailable)
    val formattedDate = image.lastModifiedMillis?.formatDate()
    val formatter = remember { StatusImagePresentationFormatter() }
    val title = formatter.title(image.name, formattedDate).ifBlank { unavailable }
    val size = formatter.sizeValue(image.sizeBytes) ?: unavailable

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.status_image_mime_type, image.mimeType),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.status_image_size, size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.status_image_date, formattedDate ?: unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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

private fun StatusImage.displayTitle(): String {
    return name.ifBlank { uri.toString() }
}

private fun Long.formatDate(): String {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(this))
}
