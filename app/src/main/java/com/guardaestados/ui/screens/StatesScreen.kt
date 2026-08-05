package com.guardaestados.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.guardaestados.domain.status.StatusGalleryState
import com.guardaestados.domain.status.StatusImage
import com.guardaestados.domain.status.StatusMediaType
import com.guardaestados.ui.status.StatusImagePresentationFormatter
import com.guardaestados.ui.theme.BrandGradientButton
import com.guardaestados.ui.theme.brandGradientBorder
import java.text.DateFormat
import java.util.Date

private const val ThumbnailPixelSize = 420

@Composable
fun StatesScreen(
    statusGalleryState: StatusGalleryState,
    onRefresh: () -> Unit,
    onImageSelected: (StatusImage) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val selectedMediaType = if (selectedTabIndex == 0) StatusMediaType.Image else StatusMediaType.Video

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                StatesHeader(
                    statusGalleryState = statusGalleryState,
                    onRefresh = onRefresh
                )
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
                    val images = statusGalleryState.images.filter { it.mediaType == StatusMediaType.Image }
                    val videos = statusGalleryState.images.filter { it.mediaType == StatusMediaType.Video }
                    val selectedItems = if (selectedMediaType == StatusMediaType.Image) images else videos
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        StatesMediaTabs(
                            selectedTabIndex = selectedTabIndex,
                            imageCount = images.size,
                            videoCount = videos.size,
                            onTabSelected = { selectedTabIndex = it }
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        StatesCountCard(
                            itemCount = selectedItems.size,
                            selectedMediaType = selectedMediaType
                        )
                    }
                    if (selectedItems.isEmpty()) {
                        fullWidthMessage(
                            titleRes = if (selectedMediaType == StatusMediaType.Video) R.string.states_empty_videos_title else R.string.states_empty_images_title,
                            bodyRes = if (selectedMediaType == StatusMediaType.Video) R.string.states_empty_videos_body else R.string.states_empty_images_body
                        )
                    } else {
                        items(
                            items = selectedItems,
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
}

@Composable
private fun StatesHeader(
    statusGalleryState: StatusGalleryState,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = brandGradientBorder(highlight = true),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.states_header_badge),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.states_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(R.string.states_subtitle),
                style = MaterialTheme.typography.bodyLarge
            )
            if (statusGalleryState is StatusGalleryState.Content) {
                Text(
                    text = stringResource(R.string.states_header_note),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            BrandGradientButton(
                text = stringResource(R.string.states_action_refresh),
                onClick = onRefresh
            )
        }
    }
}

@Composable
private fun StatesMediaTabs(
    selectedTabIndex: Int,
    imageCount: Int,
    videoCount: Int,
    onTabSelected: (Int) -> Unit
) {
    PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
        Tab(
            selected = selectedTabIndex == 0,
            onClick = { onTabSelected(0) },
            text = { Text(text = stringResource(R.string.states_tab_images, imageCount)) }
        )
        Tab(
            selected = selectedTabIndex == 1,
            onClick = { onTabSelected(1) },
            text = { Text(text = stringResource(R.string.states_tab_videos, videoCount)) }
        )
    }
}

@Composable
private fun StatesCountCard(
    itemCount: Int,
    selectedMediaType: StatusMediaType
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = brandGradientBorder(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    if (selectedMediaType == StatusMediaType.Video) R.string.states_found_videos_title else R.string.states_found_title
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.states_count_badge, itemCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        ),
        border = brandGradientBorder(),
        shape = RoundedCornerShape(8.dp)
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
    val formatValue = formatter.formatValue(image.mimeType)
    var failedToLoad by remember(image.uri) { mutableStateOf(false) }

    Card(
        modifier = Modifier.clickable(
            onClickLabel = stringResource(
                if (image.mediaType == StatusMediaType.Video) R.string.status_video_open_action else R.string.status_image_open_action,
                displayTitle
            ),
            role = Role.Button,
            onClick = { onImageSelected(image) }
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = brandGradientBorder(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.86f)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (image.mediaType == StatusMediaType.Video) {
                    VideoThumbnail(
                        image = image,
                        displayTitle = displayTitle,
                        failedToLoad = failedToLoad,
                        onLoading = { failedToLoad = false },
                        onError = { failedToLoad = true }
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
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedDate ?: unavailable,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (sizeValue != null || formatValue != null) {
                    Text(
                        text = listOfNotNull(formatValue, sizeValue).joinToString(" - "),
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

@Composable
private fun VideoThumbnail(
    image: StatusImage,
    displayTitle: String,
    failedToLoad: Boolean,
    onLoading: () -> Unit,
    onError: () -> Unit
) {
    val context = LocalContext.current
    var loaded by remember(image.uri) { mutableStateOf(false) }

    VideoFallbackThumbnail()
    if (!failedToLoad) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(image.uri)
                .size(Size(ThumbnailPixelSize, ThumbnailPixelSize))
                .build(),
            contentDescription = stringResource(R.string.status_video_thumbnail_description, displayTitle),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (loaded) 1f else 0f),
            onLoading = {
                loaded = false
                onLoading()
            },
            onSuccess = { loaded = true },
            onError = {
                loaded = false
                onError()
            }
        )
    }
    VideoPlayIndicator()
}

@Composable
private fun VideoFallbackThumbnail() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_video),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VideoPlayIndicator() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.72f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_play_arrow),
            contentDescription = null,
            modifier = Modifier.size(30.dp),
            tint = MaterialTheme.colorScheme.inverseOnSurface
        )
    }
}
private fun Long.formatDate(): String {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(this))
}
