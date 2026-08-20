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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.guardaestados.domain.saved.SavedImage
import com.guardaestados.domain.saved.SavedMediaType
import com.guardaestados.domain.saved.SavedImagesState
import com.guardaestados.ui.saved.SavedImageDeleteState
import com.guardaestados.ui.saved.SavedImagesMultiDeleteState
import com.guardaestados.ui.saved.SavedImagesMultiShareState
import com.guardaestados.domain.saved.SavedMediaOrigin
import com.guardaestados.ui.components.VideoThumbnail
import java.text.DateFormat
import java.util.Date

private const val ThumbnailPixelSize = 360
private val SavedSurfaceStrong: Color
    @Composable get() = LocalGuardaEstadosColors.current.surfaceStrong
private val SavedThumbnailBackground: Color
    @Composable get() = LocalGuardaEstadosColors.current.thumbnailBackground
private val SavedDanger: Color
    @Composable get() = LocalGuardaEstadosColors.current.danger
private val SavedDangerSoft: Color
    @Composable get() = LocalGuardaEstadosColors.current.dangerSoft
private val SavedBackground: Color
    @Composable get() = LocalGuardaEstadosColors.current.background
private val SavedSurface: Color
    @Composable get() = LocalGuardaEstadosColors.current.surface
private val SavedSurfaceSoft: Color
    @Composable get() = LocalGuardaEstadosColors.current.surfaceSoft
private val SavedTitle: Color
    @Composable get() = LocalGuardaEstadosColors.current.title
private val SavedBody: Color
    @Composable get() = LocalGuardaEstadosColors.current.body
private val SavedActive: Color
    @Composable get() = LocalGuardaEstadosColors.current.active
private val SavedBlue: Color
    @Composable get() = LocalGuardaEstadosColors.current.activeAlt
private val SavedBorder: Color
    @Composable get() = LocalGuardaEstadosColors.current.border

@Composable
fun SavedImagesScreen(
    savedImagesState: SavedImagesState,
    deleteState: SavedImageDeleteState,
    multiShareState: SavedImagesMultiShareState,
    multiDeleteState: SavedImagesMultiDeleteState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onImageSelected: (List<SavedImage>, Int) -> Unit,
    onDeleteImage: (SavedImage) -> Unit,
    onShareSelected: (List<SavedImage>) -> Unit,
    onDeleteSelected: (List<SavedImage>) -> Unit,
    onDeleteMessageDismissed: () -> Unit,
    onMultiShareMessageDismissed: () -> Unit,
    onMultiDeleteMessageDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedMediaType = if (selectedTabIndex == 0) SavedMediaType.Image else SavedMediaType.Video
    val lifecycleOwner = LocalLifecycleOwner.current
    val gridState = rememberLazyGridState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedUris by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var showMultiDeleteDialog by remember { mutableStateOf(false) }
    val selectionActive = selectedUris.isNotEmpty()
    val actionInProgress = multiShareState == SavedImagesMultiShareState.Sharing ||
        multiDeleteState is SavedImagesMultiDeleteState.Deleting
    val shareSummaryMessage = multiShareState.shareSummaryMessage()
    val deleteSummaryMessage = multiDeleteState.deleteSummaryMessage()
    val currentTabItemKeys = remember(savedImagesState, selectedMediaType) {
        val content = savedImagesState as? SavedImagesState.Content
        content?.images
            ?.filter { image -> image.mediaType == selectedMediaType }
            ?.map { image -> image.uri.toString() }
            ?.toSet()
            .orEmpty()
    }

    LaunchedEffect(selectedTabIndex) {
        selectedUris = emptySet()
        showMultiDeleteDialog = false
    }

    LaunchedEffect(currentTabItemKeys) {
        if (selectedUris.isNotEmpty()) {
            selectedUris = selectedUris.intersect(currentTabItemKeys)
        }
    }

    LaunchedEffect(shareSummaryMessage) {
        val message = shareSummaryMessage ?: return@LaunchedEffect
        selectedUris = emptySet()
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(message)
        onMultiShareMessageDismissed()
    }

    LaunchedEffect(deleteSummaryMessage) {
        val message = deleteSummaryMessage ?: return@LaunchedEffect
        selectedUris = emptySet()
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(message)
        onMultiDeleteMessageDismissed()
    }

    DisposableEffect(lifecycleOwner, onRefresh) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = SavedBackground
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 148.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 18.dp,
                    end = 16.dp,
                    bottom = if (selectionActive) 148.dp else 22.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    if (selectionActive) {
                        SavedSelectionHeader(
                            selectedCount = selectedUris.size,
                            actionInProgress = actionInProgress,
                            onCancelSelection = {
                                selectedUris = emptySet()
                                showMultiDeleteDialog = false
                            }
                        )
                    } else {
                        SavedHeader(
                            isRefreshing = isRefreshing,
                            onRefresh = onRefresh
                        )
                    }
                }

                deleteState.statusMessageRes()?.let { messageRes ->
                    if (!selectionActive) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SavedDeleteStatusCard(
                                message = stringResource(messageRes),
                                canDismiss = deleteState.canDismiss(),
                                onDismiss = onDeleteMessageDismissed
                            )
                        }
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
                        val images = savedImagesState.images.filter { it.mediaType == SavedMediaType.Image }
                        val videos = savedImagesState.images.filter { it.mediaType == SavedMediaType.Video }
                        val selectedItems = if (selectedMediaType == SavedMediaType.Image) images else videos

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SavedMediaTabs(
                                selectedTabIndex = selectedTabIndex,
                                imageCount = images.size,
                                videoCount = videos.size,
                                onTabSelected = { tabIndex ->
                                    selectedUris = emptySet()
                                    showMultiDeleteDialog = false
                                    selectedTabIndex = tabIndex
                                }
                            )
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SavedCountRow(itemCount = selectedItems.size)
                        }
                        if (selectedItems.isEmpty()) {
                            fullWidthSavedMessage(
                                titleRes = if (selectedMediaType == SavedMediaType.Video) R.string.saved_empty_videos_title else R.string.saved_empty_images_title,
                                bodyRes = if (selectedMediaType == SavedMediaType.Video) R.string.saved_empty_videos_body else R.string.saved_empty_images_body
                            )
                        } else {
                            itemsIndexed(
                                items = selectedItems,
                                key = { _, image -> image.uri.toString() }
                            ) { index, image ->
                                val imageKey = image.uri.toString()
                                SavedImageGridCard(
                                    image = image,
                                    selected = imageKey in selectedUris,
                                    selectionActive = selectionActive,
                                    enabled = !actionInProgress && deleteState != SavedImageDeleteState.Deleting,
                                    onImageSelected = {
                                        if (selectionActive) {
                                            selectedUris = selectedUris.toggle(imageKey)
                                            if (selectedUris.isEmpty()) {
                                                showMultiDeleteDialog = false
                                            }
                                        } else {
                                            onImageSelected(selectedItems, index)
                                        }
                                    },
                                    onSelectionStarted = {
                                        selectedUris = setOf(imageKey)
                                    },
                                    onDeleteImage = onDeleteImage,
                                    deleting = deleteState == SavedImageDeleteState.Deleting
                                )
                            }
                        }
                    }
                }
            }

            val content = savedImagesState as? SavedImagesState.Content
            if (selectionActive && content != null) {
                val selectedItems = content.images
                    .filter { image -> image.mediaType == selectedMediaType }
                    .filter { image -> image.uri.toString() in selectedUris }
                if (showMultiDeleteDialog) {
                    ConfirmSavedImagesDeleteDialog(
                        selectedCount = selectedItems.size,
                        onDismiss = { showMultiDeleteDialog = false },
                        onConfirm = {
                            showMultiDeleteDialog = false
                            onDeleteSelected(selectedItems)
                        }
                    )
                }
                SavedSelectionActionBar(
                    selectedCount = selectedItems.size,
                    multiShareState = multiShareState,
                    multiDeleteState = multiDeleteState,
                    onShareSelected = { onShareSelected(selectedItems) },
                    onDeleteSelected = { showMultiDeleteDialog = true },
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
private fun SavedHeader(
    isRefreshing: Boolean,
    onRefresh: () -> Unit
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
                text = stringResource(R.string.saved_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = SavedTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.saved_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = SavedBody,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(16.dp),
            color = SavedSurface,
            contentColor = SavedActive,
            border = BorderStroke(1.dp, SavedBorder),
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = !isRefreshing, role = Role.Button, onClick = onRefresh),
                contentAlignment = Alignment.Center
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = SavedActive
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = stringResource(R.string.saved_action_refresh),
                        tint = SavedActive
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedMediaTabs(
    selectedTabIndex: Int,
    imageCount: Int,
    videoCount: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SavedSurface,
        border = BorderStroke(1.dp, SavedBorder),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SavedTab(
                selected = selectedTabIndex == 0,
                text = stringResource(R.string.saved_tab_images, imageCount),
                iconRes = R.drawable.ic_image,
                onClick = { onTabSelected(0) },
                modifier = Modifier.weight(1f)
            )
            SavedTab(
                selected = selectedTabIndex == 1,
                text = stringResource(R.string.saved_tab_videos, videoCount),
                iconRes = R.drawable.ic_video,
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SavedTab(
    selected: Boolean,
    text: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) SavedActive else SavedBody
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) SavedSurfaceSoft else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
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
                .background(if (selected) SavedActive else Color.Transparent)
        )
    }
}

@Composable
private fun SavedCountRow(itemCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = SavedSurface,
            border = BorderStroke(1.dp, SavedBorder)
        ) {
            Text(
                text = stringResource(R.string.saved_found_body, itemCount),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = SavedBody,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
        colors = CardDefaults.cardColors(containerColor = SavedSurface),
        border = BorderStroke(1.dp, SavedBorder),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = SavedBody
            )
            if (canDismiss) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.saved_delete_dialog_cancel), color = SavedActive)
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
        colors = CardDefaults.cardColors(containerColor = SavedSurface),
        border = BorderStroke(1.dp, SavedBorder),
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
                color = SavedTitle
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = SavedBody
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SavedImageGridCard(
    image: SavedImage,
    selected: Boolean,
    selectionActive: Boolean,
    enabled: Boolean,
    onImageSelected: () -> Unit,
    onSelectionStarted: () -> Unit,
    onDeleteImage: (SavedImage) -> Unit,
    deleting: Boolean
) {
    val context = LocalContext.current
    val formattedDate = image.dateAddedMillis?.formatDate()
    val unavailable = stringResource(R.string.status_image_value_unavailable)
    val selectedDescription = stringResource(R.string.states_selection_item_selected)
    val notSelectedDescription = stringResource(R.string.states_selection_item_not_selected)
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
        modifier = Modifier
            .shadow(6.dp, RoundedCornerShape(20.dp), clip = false)
            .combinedClickable(
                enabled = enabled,
                onClickLabel = stringResource(
                    if (selectionActive) {
                        if (selected) R.string.states_selection_unselect_item else R.string.states_selection_select_item
                    } else if (image.mediaType == SavedMediaType.Video) {
                        R.string.saved_video_open_action_simple
                    } else {
                        R.string.saved_image_open_action_simple
                    }
                ),
                onLongClickLabel = stringResource(R.string.states_selection_start),
                role = Role.Button,
                onLongClick = {
                    if (selectionActive) {
                        onImageSelected()
                    } else {
                        onSelectionStarted()
                    }
                },
                onClick = onImageSelected
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
        colors = CardDefaults.cardColors(containerColor = SavedSurface),
        border = BorderStroke(1.dp, SavedBorder),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(7.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SavedThumbnailBackground),
            contentAlignment = Alignment.Center
        ) {
            if (image.mediaType == SavedMediaType.Video) {
                VideoThumbnail(
                    uri = image.uri,
                    contentDescription = stringResource(R.string.saved_video_card_description)
                )
                SavedBadge(
                    text = stringResource(R.string.saved_video_thumbnail_label),
                    iconRes = R.drawable.ic_play_arrow,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                )
            } else if (failedToLoad) {
                Text(
                    text = stringResource(R.string.status_image_thumbnail_error),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SavedBody
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

            if (selectionActive) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(if (selected) SavedActive.copy(alpha = 0.32f) else Color.Black.copy(alpha = 0.18f))
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) SavedActive else SavedSurfaceStrong,
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
            } else {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = SavedSurfaceStrong,
                    contentColor = SavedDanger,
                    border = BorderStroke(1.dp, SavedBorder),
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clickable(enabled = !deleting, role = Role.Button) { showDeleteDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.saved_delete_media_description),
                            modifier = Modifier.size(18.dp),
                            tint = SavedDanger
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SavedBadge(
                text = stringResource(image.origin.labelRes()),
                iconRes = if (image.origin == SavedMediaOrigin.VideoPart) R.drawable.ic_video else R.drawable.ic_nav_saved
            )
            SavedBadge(
                text = stringResource(R.string.saved_image_date, formattedDate ?: unavailable),
                iconRes = R.drawable.ic_image
            )
        }
    }
}

@Composable
private fun SavedSelectionHeader(
    selectedCount: Int,
    actionInProgress: Boolean,
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
            color = SavedSurface,
            contentColor = SavedActive,
            border = BorderStroke(1.dp, SavedBorder),
            shadowElevation = 0.dp
        ) {
            IconButton(
                onClick = onCancelSelection,
                enabled = !actionInProgress
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.states_selection_cancel),
                    tint = SavedActive
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
            color = SavedTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SavedSelectionActionBar(
    selectedCount: Int,
    multiShareState: SavedImagesMultiShareState,
    multiDeleteState: SavedImagesMultiDeleteState,
    onShareSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deleting = multiDeleteState as? SavedImagesMultiDeleteState.Deleting
    val sharing = multiShareState == SavedImagesMultiShareState.Sharing
    val actionInProgress = sharing || deleting != null
    val progress = deleting?.let { state ->
        if (state.totalCount > 0) state.processedCount.toFloat() / state.totalCount.toFloat() else 0f
    } ?: 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = SavedSurface,
        border = BorderStroke(1.dp, SavedBorder),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                deleting != null -> {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = SavedActive,
                        trackColor = SavedSurfaceSoft
                    )
                    Text(
                        text = stringResource(
                            R.string.saved_selection_deleting_progress,
                            deleting.processedCount,
                            deleting.totalCount
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = SavedBody,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                sharing -> Text(
                    text = stringResource(R.string.saved_selection_sharing_progress),
                    style = MaterialTheme.typography.labelMedium,
                    color = SavedBody,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SavedSelectionActionButton(
                    text = pluralStringResource(
                        R.plurals.saved_selection_share_action,
                        selectedCount,
                        selectedCount
                    ),
                    iconRes = R.drawable.ic_share,
                    enabled = !actionInProgress && selectedCount > 0,
                    danger = false,
                    onClick = onShareSelected,
                    modifier = Modifier.weight(1f)
                )
                SavedSelectionActionButton(
                    text = pluralStringResource(
                        R.plurals.saved_selection_delete_action,
                        selectedCount,
                        selectedCount
                    ),
                    iconRes = R.drawable.ic_delete,
                    enabled = !actionInProgress && selectedCount > 0,
                    danger = true,
                    onClick = onDeleteSelected,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SavedSelectionActionButton(
    text: String,
    iconRes: Int,
    enabled: Boolean,
    danger: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = when {
        !enabled -> SavedSurfaceSoft
        danger -> SavedDangerSoft
        else -> SavedActive
    }
    val contentColor = when {
        !enabled -> SavedBody
        danger -> SavedDanger
        else -> Color.White
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = contentColor
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SavedBadge(
    text: String,
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = SavedSurfaceStrong,
        contentColor = SavedTitle,
        border = BorderStroke(1.dp, SavedBorder),
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
                tint = SavedBlue
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = SavedTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
        containerColor = SavedSurfaceStrong,
        titleContentColor = SavedTitle,
        textContentColor = SavedBody,
        title = { Text(text = stringResource(R.string.saved_delete_dialog_title)) },
        text = { Text(text = stringResource(R.string.saved_delete_dialog_body)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SavedDangerSoft,
                    contentColor = SavedDanger
                )
            ) {
                Text(text = stringResource(R.string.saved_delete_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.saved_delete_dialog_cancel), color = SavedActive)
            }
        }
    )
}

@Composable
private fun ConfirmSavedImagesDeleteDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SavedSurfaceStrong,
        titleContentColor = SavedTitle,
        textContentColor = SavedBody,
        title = {
            Text(
                text = pluralStringResource(
                    R.plurals.saved_selection_delete_dialog_title,
                    selectedCount,
                    selectedCount
                )
            )
        },
        text = { Text(text = stringResource(R.string.saved_selection_delete_dialog_body)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SavedDangerSoft,
                    contentColor = SavedDanger
                )
            ) {
                Text(text = stringResource(R.string.saved_delete_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.saved_delete_dialog_cancel), color = SavedActive)
            }
        }
    )
}

@Composable
private fun SavedImagesMultiShareState.shareSummaryMessage(): String? {
    val finished = this as? SavedImagesMultiShareState.Finished ?: return null
    val summary = finished.summary
    return when {
        summary.failedCount == 0 -> pluralStringResource(
            R.plurals.saved_selection_shared_summary,
            summary.successCount,
            summary.successCount
        )

        summary.successCount == 0 -> stringResource(R.string.saved_selection_share_all_failed)
        else -> {
            val sharedText = pluralStringResource(
                R.plurals.saved_selection_shared_summary,
                summary.successCount,
                summary.successCount
            )
            val failedText = pluralStringResource(
                R.plurals.saved_selection_share_failed_summary,
                summary.failedCount,
                summary.failedCount
            )
            stringResource(R.string.states_selection_partial_summary, sharedText, failedText)
        }
    }
}

@Composable
private fun SavedImagesMultiDeleteState.deleteSummaryMessage(): String? {
    val finished = this as? SavedImagesMultiDeleteState.Finished ?: return null
    val summary = finished.summary
    return when {
        summary.failedCount == 0 -> pluralStringResource(
            R.plurals.saved_selection_deleted_summary,
            summary.successCount,
            summary.successCount
        )

        summary.successCount == 0 -> stringResource(R.string.saved_selection_delete_all_failed)
        else -> {
            val deletedText = pluralStringResource(
                R.plurals.saved_selection_deleted_summary,
                summary.successCount,
                summary.successCount
            )
            val failedText = pluralStringResource(
                R.plurals.saved_selection_delete_failed_summary,
                summary.failedCount,
                summary.failedCount
            )
            stringResource(R.string.states_selection_partial_summary, deletedText, failedText)
        }
    }
}

private fun SavedImageDeleteState.statusMessageRes(): Int? {
    return when (this) {
        SavedImageDeleteState.Idle -> null
        SavedImageDeleteState.Deleting -> R.string.saved_delete_status_deleting
        SavedImageDeleteState.Success -> null
        SavedImageDeleteState.AlreadyMissing -> R.string.saved_delete_status_missing
        SavedImageDeleteState.InvalidTarget -> R.string.saved_delete_status_invalid
        SavedImageDeleteState.Error -> R.string.saved_delete_status_error
        is SavedImageDeleteState.NeedsSystemConfirmation -> R.string.saved_delete_system_confirmation
    }
}

private fun SavedImageDeleteState.canDismiss(): Boolean {
    return this != SavedImageDeleteState.Deleting && this !is SavedImageDeleteState.NeedsSystemConfirmation
}

private fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) this - value else this + value
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
