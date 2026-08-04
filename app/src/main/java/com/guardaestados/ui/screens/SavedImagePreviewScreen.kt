package com.guardaestados.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.guardaestados.R
import com.guardaestados.domain.saved.SavedImage
import com.guardaestados.ui.saved.SavedImageDeleteState
import com.guardaestados.ui.saved.SavedImagePreviewState
import com.guardaestados.ui.status.StatusImagePresentationFormatter
import java.text.DateFormat
import java.util.Date

@Composable
fun SavedImagePreviewScreen(
    previewState: SavedImagePreviewState,
    deleteState: SavedImageDeleteState,
    onDeleteImage: (SavedImage) -> Unit,
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onBack) {
                Text(text = stringResource(R.string.preview_action_back))
            }

            Text(
                text = stringResource(R.string.saved_preview_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            deleteState.statusMessageRes()?.let { messageRes ->
                SavedPreviewMessageCard(
                    title = stringResource(R.string.saved_delete_dialog_title),
                    body = stringResource(messageRes)
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
                    onDeleteImage = onDeleteImage,
                    deleting = deleteState == SavedImageDeleteState.Deleting ||
                        deleteState is SavedImageDeleteState.NeedsSystemConfirmation
                )
            }
        }
    }
}

@Composable
private fun SavedPreviewContent(
    image: SavedImage,
    onDeleteImage: (SavedImage) -> Unit,
    deleting: Boolean
) {
    var failedToLoad by remember(image.uri) { mutableStateOf(false) }
    var showDeleteDialog by remember(image.uri) { mutableStateOf(false) }

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
                            R.string.saved_preview_image_description,
                            image.displayTitle()
                        ),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        onError = { failedToLoad = true }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { showDeleteDialog = true },
                    enabled = !deleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = stringResource(R.string.saved_action_delete)
                    )
                }
            }

            SavedImageInfoCard(image)
        }
    }
}

@Composable
private fun SavedImageInfoCard(image: SavedImage) {
    val unavailable = stringResource(R.string.status_image_value_unavailable)
    val formattedDate = image.dateAddedMillis?.formatDate()
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
            text = stringResource(R.string.saved_image_date, formattedDate ?: unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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

private fun SavedImage.displayTitle(): String {
    return name.ifBlank { uri.toString() }
}

private fun Long.formatDate(): String {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(this))
}
