package com.guardaestados.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.guardaestados.ui.theme.LocalGuardaEstadosColors
import com.guardaestados.R
import com.guardaestados.domain.media.MediaDetailsFormatter
import com.guardaestados.domain.status.StatusGalleryState
import com.guardaestados.domain.status.StatusImage
import com.guardaestados.domain.status.StatusMediaType
import com.guardaestados.ui.ads.AdaptiveBannerAd
import com.guardaestados.ui.save.MultiSaveStatusUiState
import com.guardaestados.ui.components.VideoThumbnail
import java.text.DateFormat
import java.util.Date

private const val ThumbnailPixelSize = 420
private val StatesBackground: Color
    @Composable get() = LocalGuardaEstadosColors.current.background
private val StatesSurface: Color
    @Composable get() = LocalGuardaEstadosColors.current.surface
private val StatesSurfaceSoft: Color
    @Composable get() = LocalGuardaEstadosColors.current.surfaceSoft
private val StatesTitle: Color
    @Composable get() = LocalGuardaEstadosColors.current.title
private val StatesBody: Color
    @Composable get() = LocalGuardaEstadosColors.current.body
private val StatesActive: Color
    @Composable get() = LocalGuardaEstadosColors.current.active
private val StatesBadgeOverlay: Color
    @Composable get() = LocalGuardaEstadosColors.current.badgeOverlay
private val StatesThumbnailBackground: Color
    @Composable get() = LocalGuardaEstadosColors.current.thumbnailBackground
private val StatesBorder: Color
    @Composable get() = LocalGuardaEstadosColors.current.border

@Composable
fun StatesScreen(
    statusGalleryState: StatusGalleryState,
    multiSaveState: MultiSaveStatusUiState,
    adsCanRequest: Boolean,
    bannerAdUnitId: String,
    onRefresh: () -> Unit,
    onSaveSelected: (List<StatusImage>) -> Unit,
    onMultiSaveMessageShown: () -> Unit,
    onImageSelected: (List<StatusImage>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedMediaType = if (selectedTabIndex == 0) StatusMediaType.Image else StatusMediaType.Video
    val gridState = rememberLazyGridState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedUris by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val selectionActive = selectedUris.isNotEmpty()
    val savingSelection = multiSaveState is MultiSaveStatusUiState.Saving
    val saveSummaryMessage = multiSaveState.saveSummaryMessage()
    val currentTabItemKeys = remember(statusGalleryState, selectedMediaType) {
        val content = statusGalleryState as? StatusGalleryState.Content
        content?.images
            ?.filter { image -> image.mediaType == selectedMediaType }
            ?.map { image -> image.uri.toString() }
            ?.toSet()
            .orEmpty()
    }

    LaunchedEffect(selectedTabIndex) {
        selectedUris = emptySet()
    }

    LaunchedEffect(currentTabItemKeys) {
        if (selectedUris.isNotEmpty()) {
            selectedUris = selectedUris.intersect(currentTabItemKeys)
        }
    }

    LaunchedEffect(saveSummaryMessage) {
        val message = saveSummaryMessage ?: return@LaunchedEffect
        selectedUris = emptySet()
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(message)
        onMultiSaveMessageShown()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = StatesBackground
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 18.dp,
                    end = 16.dp,
                    bottom = if (selectionActive) 128.dp else 22.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    if (selectionActive) {
                        StatesSelectionHeader(
                            selectedCount = selectedUris.size,
                            saving = savingSelection,
                            onCancelSelection = { selectedUris = emptySet() }
                        )
                    } else {
                        StatesHeader(
                            onRefresh = onRefresh,
                            enabled = !savingSelection
                        )
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
                        val images = statusGalleryState.images.filter { it.mediaType == StatusMediaType.Image }
                        val videos = statusGalleryState.images.filter { it.mediaType == StatusMediaType.Video }
                        val selectedItems = if (selectedMediaType == StatusMediaType.Image) images else videos
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            StatesMediaTabs(
                                selectedTabIndex = selectedTabIndex,
                                imageCount = images.size,
                                videoCount = videos.size,
                                enabled = !savingSelection,
                                onTabSelected = { tabIndex ->
                                    selectedUris = emptySet()
                                    selectedTabIndex = tabIndex
                                }
                            )
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            StatesCountRow(
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
                            itemsIndexed(
                                items = selectedItems,
                                key = { _, image -> image.uri.toString() }
                            ) { index, image ->
                                val imageKey = image.uri.toString()
                                StatusImageGridCard(
                                    image = image,
                                    selected = imageKey in selectedUris,
                                    selectionActive = selectionActive,
                                    enabled = !savingSelection,
                                    onImageSelected = {
                                        if (selectionActive) {
                                            selectedUris = selectedUris.toggle(imageKey)
                                        } else {
                                            onImageSelected(selectedItems, index)
                                        }
                                    },
                                    onSelectionStarted = {
                                        selectedUris = setOf(imageKey)
                                    }
                                )
                            }
                        }
                        if (!selectionActive && selectedItems.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                AdaptiveBannerAd(
                                    adUnitId = bannerAdUnitId,
                                    canRequestAds = adsCanRequest
                                )
                            }
                        }

                    }
                }

                if (statusGalleryState !is StatusGalleryState.Content) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        RefreshFooter(
                            onRefresh = onRefresh,
                            enabled = !savingSelection && !selectionActive
                        )
                    }
                }
            }

            val content = statusGalleryState as? StatusGalleryState.Content
            if (selectionActive && content != null) {
                val selectedItems = content.images
                    .filter { image -> image.mediaType == selectedMediaType }
                    .filter { image -> image.uri.toString() in selectedUris }
                StatesSelectionActionBar(
                    selectedCount = selectedItems.size,
                    multiSaveState = multiSaveState,
                    onSaveSelected = { onSaveSelected(selectedItems) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                snackbar = { snackbarData ->
                    Snackbar(snackbarData = snackbarData)
                }
            )
        }
    }
}

@Composable
private fun StatesHeader(
    onRefresh: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.states_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = StatesTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.states_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = StatesBody,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(16.dp),
            color = StatesSurface,
            contentColor = StatesActive,
            border = BorderStroke(1.dp, StatesBorder),
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = enabled, role = Role.Button, onClick = onRefresh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = stringResource(R.string.states_action_refresh),
                    tint = StatesActive
                )
            }
        }
    }
}

@Composable
private fun StatesMediaTabs(
    selectedTabIndex: Int,
    imageCount: Int,
    videoCount: Int,
    enabled: Boolean,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = StatesSurface,
        border = BorderStroke(1.dp, StatesBorder),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatesTab(
                selected = selectedTabIndex == 0,
                text = stringResource(R.string.states_tab_images, imageCount),
                iconRes = R.drawable.ic_image,
                enabled = enabled,
                onClick = { onTabSelected(0) },
                modifier = Modifier.weight(1f)
            )
            StatesTab(
                selected = selectedTabIndex == 1,
                text = stringResource(R.string.states_tab_videos, videoCount),
                iconRes = R.drawable.ic_video,
                enabled = enabled,
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatesTab(
    selected: Boolean,
    text: String,
    iconRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) StatesActive else StatesBody
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) StatesSurfaceSoft else Color.Transparent)
            .clickable(enabled = enabled, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
            Text(
                text = text,
                modifier = Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(0.34f)
                .size(width = 1.dp, height = 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) StatesActive else Color.Transparent)
        )
    }
}

@Composable
private fun StatesCountRow(
    itemCount: Int,
    selectedMediaType: StatusMediaType
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                if (selectedMediaType == StatusMediaType.Video) R.string.states_found_videos_title else R.string.states_found_title
            ),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = StatesTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = StatesSurface,
            border = BorderStroke(1.dp, StatesBorder)
        ) {
            Text(
                text = stringResource(R.string.states_count_badge, itemCount),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = StatesBody,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.fullWidthMessage(
    @StringRes titleRes: Int,
    bodyRes: Int
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
        colors = CardDefaults.cardColors(containerColor = StatesSurface),
        border = BorderStroke(1.dp, StatesBorder),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = StatesTitle
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = StatesBody
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun StatusImageGridCard(
    image: StatusImage,
    selected: Boolean,
    selectionActive: Boolean,
    enabled: Boolean,
    onImageSelected: (StatusImage) -> Unit,
    onSelectionStarted: (StatusImage) -> Unit
) {
    val context = LocalContext.current
    val formattedDate = image.lastModifiedMillis?.formatDate()
    val formattedVideoDuration = remember(image.durationMillis) {
        MediaDetailsFormatter().formatDuration(image.durationMillis)
    }
    val selectedDescription = stringResource(R.string.states_selection_item_selected)
    val notSelectedDescription = stringResource(R.string.states_selection_item_not_selected)
    var failedToLoad by remember(image.uri) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(20.dp), clip = false)
            .combinedClickable(
                enabled = enabled,
                onClickLabel = stringResource(
                    if (selectionActive) {
                        if (selected) R.string.states_selection_unselect_item else R.string.states_selection_select_item
                    } else if (image.mediaType == StatusMediaType.Video) {
                        R.string.status_video_open_action_simple
                    } else {
                        R.string.status_image_open_action_simple
                    }
                ),
                onLongClickLabel = stringResource(R.string.states_selection_start),
                role = Role.Button,
                onLongClick = {
                    if (selectionActive) {
                        onImageSelected(image)
                    } else {
                        onSelectionStarted(image)
                    }
                },
                onClick = { onImageSelected(image) }
            )
            .semantics {
                this.selected = selected
                if (selectionActive) {
                    contentDescription = if (selected) {
                        selectedDescription
                    } else {
                        notSelectedDescription
                    }
                }
            },
        colors = CardDefaults.cardColors(containerColor = StatesSurface),
        border = BorderStroke(1.dp, StatesBorder),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.82f)
                .padding(7.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(StatesThumbnailBackground),
            contentAlignment = Alignment.Center
        ) {
            if (image.mediaType == StatusMediaType.Video) {
                VideoThumbnail(
                    uri = image.uri,
                    contentDescription = stringResource(R.string.status_video_card_description)
                )
                if (formattedVideoDuration != null) {
                    MediaTypeBadge(
                        text = formattedVideoDuration,
                        iconRes = R.drawable.ic_play_arrow,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    )
                } else {
                    MediaTypeIconBadge(
                        iconRes = R.drawable.ic_play_arrow,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    )
                }
            } else if (failedToLoad) {
                Text(
                    text = stringResource(R.string.status_image_thumbnail_error),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = StatesBody
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(image.uri)
                        .size(Size(ThumbnailPixelSize, ThumbnailPixelSize))
                        .build(),
                    contentDescription = stringResource(
                        R.string.status_image_thumbnail_description,
                        stringResource(R.string.status_image_card_description)
                    ),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onError = { failedToLoad = true }
                )
                if (formattedDate != null) {
                    MediaTypeBadge(
                        text = formattedDate,
                        iconRes = R.drawable.ic_image,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    )
                }
            }
            if (selectionActive) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(if (selected) StatesActive.copy(alpha = 0.32f) else Color.Black.copy(alpha = 0.18f))
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) StatesActive else StatesBadgeOverlay,
                    contentColor = Color.White,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.72f)),
                    shadowElevation = 0.dp
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(
                            if (selected) {
                                R.string.states_selection_item_selected
                            } else {
                                R.string.states_selection_item_not_selected
                            }
                        ),
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp),
                        tint = if (selected) Color.White else Color.Transparent
                    )
                }
            }
        }
    }
}

@Composable
private fun StatesSelectionHeader(
    selectedCount: Int,
    saving: Boolean,
    onCancelSelection: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(16.dp),
            color = StatesSurface,
            contentColor = StatesActive,
            border = BorderStroke(1.dp, StatesBorder),
            shadowElevation = 0.dp
        ) {
            IconButton(
                onClick = onCancelSelection,
                enabled = !saving
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.states_selection_cancel),
                    tint = StatesActive
                )
            }
        }
        Text(
            text = pluralStringResource(
                R.plurals.states_selection_count,
                selectedCount,
                selectedCount
            ),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = StatesTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatesSelectionActionBar(
    selectedCount: Int,
    multiSaveState: MultiSaveStatusUiState,
    onSaveSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val saving = multiSaveState as? MultiSaveStatusUiState.Saving
    val progress = saving?.let { state ->
        if (state.totalCount > 0) state.processedCount.toFloat() / state.totalCount.toFloat() else 0f
    } ?: 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = StatesSurface,
        border = BorderStroke(1.dp, StatesBorder),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (saving != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = StatesActive,
                    trackColor = StatesSurfaceSoft
                )
                Text(
                    text = stringResource(
                        R.string.states_selection_saving_progress,
                        saving.processedCount,
                        saving.totalCount
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = StatesBody,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (saving == null) StatesActive else StatesSurfaceSoft)
                    .clickable(
                        enabled = saving == null && selectedCount > 0,
                        role = Role.Button,
                        onClick = onSaveSelected
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_saved),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (saving == null) Color.White else StatesBody
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.states_selection_save_action,
                        selectedCount,
                        selectedCount
                    ),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (saving == null) Color.White else StatesBody,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MediaTypeIconBadge(
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = StatesBadgeOverlay,
        contentColor = StatesTitle,
        border = BorderStroke(1.dp, StatesBorder),
        shadowElevation = 0.dp
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier
                .padding(7.dp)
                .size(14.dp),
            tint = StatesActive
        )
    }
}

@Composable
private fun MediaTypeBadge(
    text: String,
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = StatesBadgeOverlay,
        contentColor = StatesTitle,
        border = BorderStroke(1.dp, StatesBorder),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = StatesActive
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = StatesTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RefreshFooter(
    onRefresh: () -> Unit,
    enabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        color = StatesSurface,
        contentColor = StatesActive,
        border = BorderStroke(1.dp, StatesBorder),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, role = Role.Button, onClick = onRefresh)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_refresh),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = StatesActive
            )
            Text(
                text = stringResource(R.string.states_refresh_footer),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = StatesActive,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MultiSaveStatusUiState.saveSummaryMessage(): String? {
    val finished = this as? MultiSaveStatusUiState.Finished ?: return null
    val summary = finished.summary
    return when {
        summary.failedCount == 0 -> pluralStringResource(
            R.plurals.states_selection_saved_summary,
            summary.savedCount,
            summary.savedCount
        )

        summary.savedCount == 0 -> stringResource(R.string.states_selection_save_all_failed)
        else -> {
            val savedText = pluralStringResource(
                R.plurals.states_selection_saved_summary,
                summary.savedCount,
                summary.savedCount
            )
            val failedText = pluralStringResource(
                R.plurals.states_selection_failed_summary,
                summary.failedCount,
                summary.failedCount
            )
            stringResource(R.string.states_selection_partial_summary, savedText, failedText)
        }
    }
}

private fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) this - value else this + value
}

private fun Long.formatDate(): String {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(this))
}
