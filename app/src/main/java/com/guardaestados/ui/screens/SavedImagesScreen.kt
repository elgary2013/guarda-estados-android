package com.guardaestados.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.height
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
import com.guardaestados.ui.saved.SavedMediaImportState
import com.guardaestados.ui.components.VideoThumbnail
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ThumbnailPixelSize = 360
private val SavedHeaderReservedHeight = 146.dp
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
    importState: SavedMediaImportState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onImportFile: () -> Unit,
    onImageSelected: (List<SavedImage>, Int) -> Unit,
    onDeleteImage: (SavedImage) -> Unit,
    onShareSelected: (List<SavedImage>) -> Unit,
    onDeleteSelected: (List<SavedImage>) -> Unit,
    onDeleteMessageDismissed: () -> Unit,
    onMultiShareMessageDismissed: () -> Unit,
    onMultiDeleteMessageDismissed: () -> Unit,
    onImportMessageDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedMediaType = if (selectedTabIndex == 0) SavedMediaType.Image else SavedMediaType.Video
    val lifecycleOwner = LocalLifecycleOwner.current
    val gridState = rememberLazyGridState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedUris by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var showMultiDeleteDialog by remember { mutableStateOf(false) }
    var manualRefreshRequested by remember { mutableStateOf(false) }
    val selectionActive = selectedUris.isNotEmpty()
    val actionInProgress = multiShareState == SavedImagesMultiShareState.Sharing ||
        multiDeleteState is SavedImagesMultiDeleteState.Deleting ||
        importState == SavedMediaImportState.Importing
    val shareSummaryMessage = multiShareState.shareSummaryMessage()
    val deleteSummaryMessage = multiDeleteState.deleteSummaryMessage()
    val deleteMessage = deleteState.deleteSnackbarMessage()
    val importMessage = importState.importMessage()
    val content = savedImagesState as? SavedImagesState.Content
    val images = content?.images?.filter { it.mediaType == SavedMediaType.Image }.orEmpty()
    val videos = content?.images?.filter { it.mediaType == SavedMediaType.Video }.orEmpty()
    val selectedItems = if (selectedMediaType == SavedMediaType.Image) images else videos
    val currentTabItemKeys = remember(savedImagesState, selectedMediaType) {
        selectedItems
            .map { image -> image.uri.toString() }
            .toSet()
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

    LaunchedEffect(deleteMessage) {
        val message = deleteMessage ?: return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(message)
        onDeleteMessageDismissed()
    }

    LaunchedEffect(importMessage) {
        val message = importMessage ?: return@LaunchedEffect
        selectedUris = emptySet()
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(message)
        onImportMessageDismissed()
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            manualRefreshRequested = false
        }
    }

    BackHandler(enabled = selectionActive) {
        selectedUris = emptySet()
        showMultiDeleteDialog = false
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
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SavedHeader(
                    onRefresh = {
                        manualRefreshRequested = true
                        onRefresh()
                    },
                    onImportFile = {
                        if (importState != SavedMediaImportState.Importing) {
                            onImportFile()
                        }
                    },
                    modifier = Modifier
                        .padding(start = 16.dp, top = 18.dp, end = 16.dp)
                )
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (savedImagesState) {
                        SavedImagesState.Loading -> SavedStateMessage(
                            titleRes = R.string.saved_loading_title,
                            bodyRes = R.string.saved_loading_body
                        )

                        SavedImagesState.Empty -> SavedStateMessage(
                            titleRes = R.string.saved_empty_title,
                            bodyRes = R.string.saved_empty_body
                        )

                        SavedImagesState.RecoverableError -> SavedStateMessage(
                            titleRes = R.string.saved_error_title,
                            bodyRes = R.string.saved_error_body
                        )

                        is SavedImagesState.Content -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 148.dp),
                                state = gridState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    top = 0.dp,
                                    end = 16.dp,
                                    bottom = if (selectionActive) 148.dp else 22.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
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
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (manualRefreshRequested && isRefreshing) {
                        SavedRefreshOverlay(modifier = Modifier.align(Alignment.TopCenter))
                    }
                }
            }

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
    onRefresh: () -> Unit,
    onImportFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(SavedHeaderReservedHeight),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        .clickable(role = Role.Button, onClick = onRefresh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = stringResource(R.string.saved_action_refresh),
                        tint = SavedActive
                    )
                }
            }
        }
        Button(
            onClick = onImportFile,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SavedActive,
                contentColor = Color.White
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nav_saved),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.saved_import_action),
                modifier = Modifier.padding(start = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
private fun SavedStateMessage(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        SavedMessageCard(
            title = stringResource(titleRes),
            body = stringResource(bodyRes)
        )
    }
}

@Composable
private fun SavedRefreshOverlay(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(top = 8.dp),
        shape = RoundedCornerShape(999.dp),
        color = SavedSurfaceStrong,
        border = BorderStroke(1.dp, SavedBorder),
        shadowElevation = 4.dp
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .padding(10.dp)
                .size(20.dp),
            strokeWidth = 2.dp,
            color = SavedActive
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
    onSelectionStarted: () -> Unit
) {
    val context = LocalContext.current
    val formattedDate = image.dateAddedMillis?.formatDate()
    val selectedDescription = stringResource(R.string.states_selection_item_selected)
    val notSelectedDescription = stringResource(R.string.states_selection_item_not_selected)
    val thumbnailFallback = stringResource(
        if (image.mediaType == SavedMediaType.Video) {
            R.string.saved_video_thumbnail_fallback
        } else {
            R.string.saved_image_thumbnail_fallback
        }
    )
    var failedToLoad by remember(image.uri) { mutableStateOf(false) }
    val thumbnailDescription = stringResource(
        R.string.saved_media_thumbnail_description,
        image.name.ifBlank { thumbnailFallback }
    )

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
                    contentDescription = thumbnailDescription
                )
                SavedVideoIndicator(
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
                    contentDescription = thumbnailDescription,
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
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp, top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = formattedDate ?: stringResource(R.string.status_image_value_unavailable),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium,
                color = SavedBody,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
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
private fun SavedVideoIndicator(
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_play_arrow),
                contentDescription = stringResource(R.string.saved_video_indicator_description),
                modifier = Modifier.size(16.dp),
                tint = SavedBlue
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

@Composable
private fun SavedMediaImportState.importMessage(): String? {
    return when (this) {
        SavedMediaImportState.Idle,
        SavedMediaImportState.Importing -> null
        is SavedMediaImportState.Success -> stringResource(R.string.saved_import_success, displayName)
        SavedMediaImportState.Unsupported -> stringResource(R.string.saved_import_unsupported)
        SavedMediaImportState.Missing -> stringResource(R.string.saved_import_missing)
        SavedMediaImportState.DestinationPermissionLost -> stringResource(R.string.saved_import_destination_permission_lost)
        SavedMediaImportState.DestinationUnavailable -> stringResource(R.string.saved_import_destination_unavailable)
        SavedMediaImportState.Error -> stringResource(R.string.saved_import_error)
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

@Composable
private fun SavedImageDeleteState.deleteSnackbarMessage(): String? {
    return when (this) {
        SavedImageDeleteState.Idle,
        SavedImageDeleteState.Deleting,
        is SavedImageDeleteState.NeedsSystemConfirmation -> null
        SavedImageDeleteState.Success -> stringResource(R.string.saved_delete_status_success)
        SavedImageDeleteState.AlreadyMissing -> stringResource(R.string.saved_delete_status_missing)
        SavedImageDeleteState.InvalidTarget -> stringResource(R.string.saved_delete_status_invalid)
        SavedImageDeleteState.Error -> stringResource(R.string.saved_delete_status_error)
    }
}

private fun SavedImageDeleteState.canDismiss(): Boolean {
    return this != SavedImageDeleteState.Deleting && this !is SavedImageDeleteState.NeedsSystemConfirmation
}

private fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) this - value else this + value
}

private fun Long.formatDate(): String {
    return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(this))
}
