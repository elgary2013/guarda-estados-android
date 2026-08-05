package com.guardaestados.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.guardaestados.R
import com.guardaestados.domain.saved.SavedImage
import com.guardaestados.domain.saved.SavedMediaType
import com.guardaestados.domain.saved.SavedImagesState
import com.guardaestados.ui.saved.SavedImageDeleteState
import com.guardaestados.domain.saved.SavedMediaOrigin
import com.guardaestados.ui.components.VideoThumbnail
import com.guardaestados.ui.theme.BrandGradientButton
import com.guardaestados.ui.theme.brandGradientBorder
import java.text.DateFormat
import java.util.Date

private const val ThumbnailPixelSize = 360

@Composable
fun SavedImagesScreen(
    savedImagesState: SavedImagesState,
    deleteState: SavedImageDeleteState,
    onRefresh: () -> Unit,
    onImageSelected: (SavedImage) -> Unit,
    onDeleteImage: (SavedImage) -> Unit,
    onDeleteMessageDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 148.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.saved_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.saved_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BrandGradientButton(
                        text = stringResource(R.string.saved_action_refresh),
                        onClick = onRefresh
                    )
                }
            }

            deleteState.statusMessageRes()?.let { messageRes ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SavedDeleteStatusCard(
                        message = stringResource(messageRes),
                        canDismiss = deleteState.canDismiss(),
                        onDismiss = onDeleteMessageDismissed
                    )
                }
            }

            when (savedImagesState) {
                SavedImagesState.Loading -> fullWidthSavedMessage(
                    titleRes = R.string.saved_loading_title,
                    bodyRes = R.string.saved_loading_body
                )

                SavedImagesState.Empty -> fullWidthSavedMessage(
                    titleRes = R.string.saved_empty_title,
                    bodyRes = R.string.saved_empty_body
                )

                SavedImagesState.RecoverableError -> fullWidthSavedMessage(
                    titleRes = R.string.saved_error_title,
                    bodyRes = R.string.saved_error_body
                )

                is SavedImagesState.Content -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SavedMessageCard(
                            title = stringResource(R.string.saved_found_title),
                            body = stringResource(
                                R.string.saved_found_body,
                                savedImagesState.images.size
                            )
                        )
                    }
                    items(
                        items = savedImagesState.images,
                        key = { image -> image.uri.toString() }
                    ) { image ->
                        SavedImageGridCard(
                            image = image,
                            onImageSelected = onImageSelected,
                            onDeleteImage = onDeleteImage,
                            deleting = deleteState == SavedImageDeleteState.Deleting
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.fullWidthSavedMessage(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        SavedMessageCard(
            title = stringResource(titleRes),
            body = stringResource(bodyRes)
        )
    }
}

@Composable
private fun SavedDeleteStatusCard(
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

@Composable
private fun SavedMessageCard(
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
private fun SavedImageGridCard(
    image: SavedImage,
    onImageSelected: (SavedImage) -> Unit,
    onDeleteImage: (SavedImage) -> Unit,
    deleting: Boolean
) {
    val context = LocalContext.current
    val formattedDate = image.dateAddedMillis?.formatDate()
    val unavailable = stringResource(R.string.status_image_value_unavailable)
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
        modifier = Modifier.clickable(
            enabled = !deleting,
            onClickLabel = stringResource(
                if (image.mediaType == SavedMediaType.Video) R.string.saved_video_open_action_simple else R.string.saved_image_open_action_simple
            ),
            role = Role.Button,
            onClick = { onImageSelected(image) }
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (image.mediaType == SavedMediaType.Video) {
                    VideoThumbnail(
                        uri = image.uri,
                        contentDescription = stringResource(R.string.saved_video_card_description)
                    )
                } else if (failedToLoad) {
                    Text(
                        text = stringResource(R.string.status_image_thumbnail_error),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(image.uri)
                            .size(Size(ThumbnailPixelSize, ThumbnailPixelSize))
                            .build(),
                        contentDescription = stringResource(
                            R.string.saved_thumbnail_description,
                            stringResource(R.string.saved_image_card_description)
                        ),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onError = { failedToLoad = true }
                    )
                }

                IconButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !deleting,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.saved_delete_media_description),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Column(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(image.origin.labelRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.saved_image_date,
                        formattedDate ?: unavailable
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ConfirmSavedImageDeleteDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.saved_delete_dialog_title)) },
        text = { Text(text = stringResource(R.string.saved_delete_dialog_body)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(text = stringResource(R.string.saved_delete_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.saved_delete_dialog_cancel))
            }
        }
    )
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

private fun SavedImageDeleteState.canDismiss(): Boolean {
    return this != SavedImageDeleteState.Deleting && this !is SavedImageDeleteState.NeedsSystemConfirmation
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
