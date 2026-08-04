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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.guardaestados.R
import com.guardaestados.domain.status.StatusGalleryState
import com.guardaestados.domain.status.StatusImage
import com.guardaestados.ui.status.StatusImagePresentationFormatter
import java.text.DateFormat
import java.util.Date

private const val ThumbnailPixelSize = 360

@Composable
fun StatesScreen(
    statusGalleryState: StatusGalleryState,
    onRefresh: () -> Unit,
    onImageSelected: (StatusImage) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 148.dp),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.states_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.states_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onRefresh) {
                        Text(text = stringResource(R.string.states_action_refresh))
                    }
                }
            }

            when (statusGalleryState) {
                StatusGalleryState.Loading -> fullWidthMessage(
                    titleRes = R.string.states_loading_title,
                    bodyRes = R.string.states_loading_body
                )

                StatusGalleryState.NoFolderSelected -> fullWidthMessage(
                    titleRes = R.string.states_no_folder_title,
                    bodyRes = R.string.states_no_folder_body
                )

                StatusGalleryState.PermissionLost -> fullWidthMessage(
                    titleRes = R.string.states_permission_lost_title,
                    bodyRes = R.string.states_permission_lost_body
                )

                StatusGalleryState.Empty -> fullWidthMessage(
                    titleRes = R.string.states_empty_title,
                    bodyRes = R.string.states_empty_body
                )

                StatusGalleryState.RecoverableError -> fullWidthMessage(
                    titleRes = R.string.states_error_title,
                    bodyRes = R.string.states_error_body
                )

                is StatusGalleryState.Content -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        GalleryMessageCard(
                            title = stringResource(R.string.states_found_title),
                            body = stringResource(
                                R.string.states_found_body,
                                statusGalleryState.images.size
                            )
                        )
                    }
                    items(
                        items = statusGalleryState.images,
                        key = { image -> image.uri.toString() }
                    ) { image ->
                        StatusImageGridCard(
                            image = image,
                            onImageSelected = onImageSelected
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.fullWidthMessage(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        GalleryMessageCard(
            title = stringResource(titleRes),
            body = stringResource(bodyRes)
        )
    }
}

@Composable
private fun GalleryMessageCard(
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
private fun StatusImageGridCard(
    image: StatusImage,
    onImageSelected: (StatusImage) -> Unit
) {
    val context = LocalContext.current
    val formatter = remember { StatusImagePresentationFormatter() }
    val formattedDate = image.lastModifiedMillis?.formatDate()
    val unavailable = stringResource(R.string.status_image_value_unavailable)
    val title = formatter.title(image.name, formattedDate)
    val displayTitle = if (title.isBlank()) unavailable else title
    val sizeValue = formatter.sizeValue(image.sizeBytes)
    var failedToLoad by remember(image.uri) { mutableStateOf(false) }

    Card(
        modifier = Modifier.clickable(
            onClickLabel = stringResource(R.string.status_image_open_action, displayTitle),
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
                if (failedToLoad) {
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
                            R.string.status_image_thumbnail_description,
                            displayTitle
                        ),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onError = { failedToLoad = true }
                    )
                }
            }

            Column(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.status_image_card_date,
                        formattedDate ?: unavailable
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (sizeValue != null) {
                    Text(
                        text = stringResource(R.string.status_image_size, sizeValue),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun Long.formatDate(): String {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(this))
}
