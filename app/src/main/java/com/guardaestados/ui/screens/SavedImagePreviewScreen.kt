package com.guardaestados.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.request.ImageRequest
import com.guardaestados.ui.theme.LocalGuardaEstadosColors
import com.guardaestados.R
import com.guardaestados.domain.saved.SavedImage
import com.guardaestados.domain.saved.SavedMediaOrigin
import com.guardaestados.domain.saved.SavedMediaType
import com.guardaestados.ui.components.ZoomableAsyncImage
import com.guardaestados.ui.saved.SavedImageDeleteState
import com.guardaestados.ui.saved.SavedImageOpenState
import com.guardaestados.ui.saved.SavedImagePreviewState
import com.guardaestados.ui.saved.SavedImageShareState
import com.guardaestados.ui.status.StatusImagePresentationFormatter
import com.guardaestados.ui.video.VideoPlayerPreview
import java.text.DateFormat
import java.util.Date

private val SavedPreviewBackground: Color
    @Composable get() = LocalGuardaEstadosColors.current.background
private val SavedPreviewSurface: Color
    @Composable get() = LocalGuardaEstadosColors.current.surface
private val SavedPreviewSurfaceSoft: Color
    @Composable get() = LocalGuardaEstadosColors.current.surfaceStrong
private val SavedPreviewTitle: Color
    @Composable get() = LocalGuardaEstadosColors.current.title
private val SavedPreviewBody: Color
    @Composable get() = LocalGuardaEstadosColors.current.body
private val SavedPreviewBorder: Color
    @Composable get() = LocalGuardaEstadosColors.current.border
private val SavedPreviewActive: Color
    @Composable get() = LocalGuardaEstadosColors.current.active
private val SavedPreviewBlue: Color
    @Composable get() = LocalGuardaEstadosColors.current.activeAlt
private val SavedPreviewGradient: Brush
    @Composable get() = LocalGuardaEstadosColors.current.primaryGradient
private val SavedPreviewGreenSoft: Color
    @Composable get() = LocalGuardaEstadosColors.current.surfaceSoft
private val SavedPreviewMediaBackground: Color
    @Composable get() = LocalGuardaEstadosColors.current.mediaBackground
private val SavedPreviewDanger: Color
    @Composable get() = LocalGuardaEstadosColors.current.danger
private val SavedPreviewDangerSoft: Color
    @Composable get() = LocalGuardaEstadosColors.current.dangerSoft
private val SavedImmersivePreviewBackground = Color(0xFF050607)
private val SavedImmersiveOverlay = Color.Black.copy(alpha = 0.48f)
private val SavedImmersiveContent = Color.White
private val SavedImmersiveMutedContent = Color.White.copy(alpha = 0.72f)
private val SavedImmersiveSnackbarContainer = Color(0xFFEAF3EF)
private val SavedImmersiveSnackbarContent = Color(0xFF101615)

@Composable
fun SavedImagePreviewScreen(
    previewState: SavedImagePreviewState,
    previewItems: List<SavedImage>,
    initialIndex: Int,
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

    val activeImage = (previewState as? SavedImagePreviewState.Content)?.image
    val useImmersivePreview = activeImage?.mediaType == SavedMediaType.Image ||
        activeImage?.mediaType == SavedMediaType.Video
    val screenBackground = if (useImmersivePreview) SavedImmersivePreviewBackground else SavedPreviewBackground

    Surface(
        modifier = modifier.fillMaxSize(),
        color = screenBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (useImmersivePreview) {
                        Modifier
                    } else {
                        Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                            .navigationBarsPadding()
                    }
                ),
            verticalArrangement = if (useImmersivePreview) Arrangement.Top else Arrangement.spacedBy(16.dp)
        ) {
            if (!useImmersivePreview) {
                SavedPreviewTopBar(onBack = onBack)
            }

            if (!useImmersivePreview) {
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

                is SavedImagePreviewState.Content -> SavedPreviewPagerContent(
                    images = previewItems.ifEmpty { listOf(previewState.image) },
                    initialIndex = initialIndex,
                    deleteState = deleteState,
                    shareState = shareState,
                    openState = openState,
                    onDeleteImage = onDeleteImage,
                    onShareImage = onShareImage,
                    onOpenImage = onOpenImage,
                    onBack = onBack,
                    onShareMessageDismissed = onShareMessageDismissed,
                    onOpenMessageDismissed = onOpenMessageDismissed,
                    immersivePreview = previewState.image.mediaType == SavedMediaType.Image ||
                        previewState.image.mediaType == SavedMediaType.Video
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.SavedPreviewPagerContent(
    images: List<SavedImage>,
    initialIndex: Int,
    deleteState: SavedImageDeleteState,
    shareState: SavedImageShareState,
    openState: SavedImageOpenState,
    onDeleteImage: (SavedImage) -> Unit,
    onShareImage: (SavedImage) -> Unit,
    onOpenImage: (SavedImage) -> Unit,
    onBack: () -> Unit,
    onShareMessageDismissed: () -> Unit,
    onOpenMessageDismissed: () -> Unit,
    immersivePreview: Boolean
) {
    if (images.isEmpty()) {
        SavedPreviewMessageCard(
            title = stringResource(R.string.saved_preview_unavailable_title),
            body = stringResource(R.string.saved_preview_unavailable_body)
        )
        return
    }

    val pageKeys = remember(images) { images.joinToString(separator = "|") { image -> image.uri.toString() } }
    val startPage = initialIndex.coerceIn(images.indices)
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { images.size })
    var activePageZoomed by remember(pageKeys) { mutableStateOf(false) }
    var activePageVideoSeeking by remember(pageKeys) { mutableStateOf(false) }

    LaunchedEffect(pageKeys, startPage) {
        if (pagerState.currentPage != startPage) {
            pagerState.scrollToPage(startPage)
        }
    }

    LaunchedEffect(pageKeys, pagerState.currentPage) {
        activePageZoomed = false
        activePageVideoSeeking = false
    }

    if (immersivePreview) {
        val snackbarHostState = remember { SnackbarHostState() }
        var deleteSnackbarRequested by remember { mutableStateOf(false) }
        var shareSnackbarRequested by remember { mutableStateOf(false) }
        var openSnackbarRequested by remember { mutableStateOf(false) }
        val deleteInvalidMessage = stringResource(R.string.saved_delete_status_invalid)
        val deleteErrorMessage = stringResource(R.string.saved_delete_status_error)
        val shareMissingMessage = stringResource(R.string.saved_share_status_missing)
        val shareInvalidMessage = stringResource(R.string.saved_share_status_invalid)
        val shareNoCompatibleAppMessage = stringResource(R.string.saved_share_status_no_app)
        val shareErrorMessage = stringResource(R.string.saved_share_status_error)
        val openMissingMessage = stringResource(R.string.saved_share_status_missing)
        val openInvalidMessage = stringResource(R.string.saved_share_status_invalid)
        val openNoCompatibleAppMessage = stringResource(R.string.saved_open_status_no_app)
        val openErrorMessage = stringResource(R.string.saved_open_status_error)
        var showDeleteDialog by remember { mutableStateOf(false) }

        LaunchedEffect(deleteState) {
            if (!deleteSnackbarRequested) return@LaunchedEffect
            val message = when (deleteState) {
                SavedImageDeleteState.Idle,
                SavedImageDeleteState.Deleting,
                SavedImageDeleteState.Success,
                SavedImageDeleteState.AlreadyMissing,
                is SavedImageDeleteState.NeedsSystemConfirmation -> null
                SavedImageDeleteState.InvalidTarget -> deleteInvalidMessage
                SavedImageDeleteState.Error -> deleteErrorMessage
            }
            if (message != null) {
                deleteSnackbarRequested = false
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(message)
            }
        }

        LaunchedEffect(shareState) {
            if (!shareSnackbarRequested) return@LaunchedEffect
            val message = when (shareState) {
                SavedImageShareState.Idle,
                SavedImageShareState.Sharing -> null
                SavedImageShareState.ChooserOpened -> {
                    shareSnackbarRequested = false
                    onShareMessageDismissed()
                    null
                }
                SavedImageShareState.AlreadyMissing -> shareMissingMessage
                SavedImageShareState.InvalidTarget -> shareInvalidMessage
                SavedImageShareState.NoCompatibleApp -> shareNoCompatibleAppMessage
                SavedImageShareState.Error -> shareErrorMessage
            }
            if (message != null) {
                shareSnackbarRequested = false
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(message)
                onShareMessageDismissed()
            }
        }

        LaunchedEffect(openState) {
            if (!openSnackbarRequested) return@LaunchedEffect
            val message = when (openState) {
                SavedImageOpenState.Idle,
                SavedImageOpenState.Opening -> null
                SavedImageOpenState.ViewerOpened -> {
                    openSnackbarRequested = false
                    onOpenMessageDismissed()
                    null
                }
                SavedImageOpenState.AlreadyMissing -> openMissingMessage
                SavedImageOpenState.InvalidTarget -> openInvalidMessage
                SavedImageOpenState.NoCompatibleApp -> openNoCompatibleAppMessage
                SavedImageOpenState.Error -> openErrorMessage
            }
            if (message != null) {
                openSnackbarRequested = false
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(message)
                onOpenMessageDismissed()
            }
        }

        if (showDeleteDialog) {
            ConfirmSavedImageDeleteDialog(
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    showDeleteDialog = false
                    deleteSnackbarRequested = true
                    onDeleteImage(images[pagerState.currentPage])
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(SavedImmersivePreviewBackground)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = images.size > 1 && !activePageZoomed && !activePageVideoSeeking
            ) { page ->
                val pageImage = images[page]
                if (pageImage.mediaType == SavedMediaType.Video) {
                    SavedImmersiveVideoPage(
                        image = pageImage,
                        isActivePage = page == pagerState.currentPage,
                        onSeekInteractionChanged = { seeking ->
                            if (page == pagerState.currentPage) {
                                activePageVideoSeeking = seeking
                            }
                        }
                    )
                } else {
                    SavedImmersiveImagePage(
                        image = pageImage,
                        isActivePage = page == pagerState.currentPage,
                        onImageZoomChanged = { zoomed ->
                            if (page == pagerState.currentPage) {
                                activePageZoomed = zoomed
                            }
                        }
                    )
                }
            }

            SavedImmersiveTopBar(
                currentPage = pagerState.currentPage,
                pageCount = images.size,
                shareState = shareState,
                openState = openState,
                deleteState = deleteState,
                onBack = onBack,
                onShareImage = {
                    shareSnackbarRequested = true
                    onShareImage(images[pagerState.currentPage])
                },
                onOpenImage = {
                    openSnackbarRequested = true
                    onOpenImage(images[pagerState.currentPage])
                },
                onDeleteImage = { showDeleteDialog = true },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = SavedImmersiveSnackbarContainer,
                        contentColor = SavedImmersiveSnackbarContent
                    )
                }
            )
        }
    } else {
        Text(
            text = stringResource(R.string.preview_page_indicator, pagerState.currentPage + 1, images.size),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = SavedPreviewBody
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            userScrollEnabled = images.size > 1 && !activePageZoomed
        ) { page ->
            SavedPreviewContent(
                image = images[page],
                isActivePage = page == pagerState.currentPage,
                onImageZoomChanged = { zoomed ->
                    if (page == pagerState.currentPage) {
                        activePageZoomed = zoomed
                    }
                },
                deleteState = deleteState,
                shareState = shareState,
                openState = openState,
                onDeleteImage = onDeleteImage,
                onShareImage = onShareImage,
                onOpenImage = onOpenImage,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
private fun SavedImmersiveImagePage(
    image: SavedImage,
    isActivePage: Boolean,
    onImageZoomChanged: (Boolean) -> Unit
) {
    var failedToLoad by remember(image.uri) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SavedImmersivePreviewBackground),
        contentAlignment = Alignment.Center
    ) {
        if (failedToLoad) {
            Text(
                text = stringResource(R.string.preview_load_error_body),
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = SavedImmersiveMutedContent
            )
        } else {
            ZoomableAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image.uri)
                    .build(),
                contentDescription = stringResource(R.string.saved_image_card_description),
                resetKey = "${image.uri}-$isActivePage",
                onZoomChanged = onImageZoomChanged,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .background(SavedImmersivePreviewBackground),
                onError = { failedToLoad = true }
            )
        }
    }
}

@Composable
private fun SavedImmersiveVideoPage(
    image: SavedImage,
    isActivePage: Boolean,
    onSeekInteractionChanged: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SavedImmersivePreviewBackground),
        contentAlignment = Alignment.Center
    ) {
        if (isActivePage) {
            VideoPlayerPreview(
                uri = image.uri,
                modifier = Modifier
                    .fillMaxSize()
                    .background(SavedImmersivePreviewBackground),
                showPlaybackStatus = true,
                errorMessage = stringResource(R.string.preview_video_load_error_body),
                immersiveControls = true,
                onSeekInteractionChanged = onSeekInteractionChanged
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_play_arrow),
                contentDescription = stringResource(R.string.preview_video_inactive_description),
                modifier = Modifier.size(56.dp),
                tint = SavedImmersiveMutedContent
            )
        }
    }
}

@Composable
private fun SavedImmersiveTopBar(
    currentPage: Int,
    pageCount: Int,
    shareState: SavedImageShareState,
    openState: SavedImageOpenState,
    deleteState: SavedImageDeleteState,
    onBack: () -> Unit,
    onShareImage: () -> Unit,
    onOpenImage: () -> Unit,
    onDeleteImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val counterText = stringResource(R.string.preview_page_indicator, currentPage + 1, pageCount)
    val deleting = deleteState == SavedImageDeleteState.Deleting ||
        deleteState is SavedImageDeleteState.NeedsSystemConfirmation
    val busy = deleting ||
        shareState == SavedImageShareState.Sharing ||
        openState == SavedImageOpenState.Opening

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SavedImmersiveOverlay)
            .statusBarsPadding()
            .padding(start = 6.dp, top = 6.dp, end = 6.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.preview_action_back),
                tint = SavedImmersiveContent
            )
        }
        SavedSegmentedProgressIndicator(
            currentPage = currentPage,
            pageCount = pageCount,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = counterText,
            modifier = Modifier
                .widthIn(min = 48.dp)
                .semantics { contentDescription = counterText },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = SavedImmersiveContent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.preview_more_options),
                    tint = SavedImmersiveContent
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.saved_action_share_media)) },
                    enabled = !busy,
                    onClick = {
                        menuExpanded = false
                        onShareImage()
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.saved_action_open_file)) },
                    enabled = !busy,
                    onClick = {
                        menuExpanded = false
                        onOpenImage()
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.saved_action_delete_copy)) },
                    enabled = !busy,
                    onClick = {
                        menuExpanded = false
                        onDeleteImage()
                    }
                )
            }
        }
    }
}

@Composable
private fun SavedSegmentedProgressIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount.coerceAtLeast(1)) { index ->
            val color = if (index <= currentPage) SavedImmersiveContent else SavedImmersiveContent.copy(alpha = 0.42f)
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 3.dp)
                    .background(color, RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
private fun SavedPreviewTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = SavedPreviewSurface,
            contentColor = SavedPreviewActive,
            border = BorderStroke(1.dp, SavedPreviewBorder),
            shadowElevation = 0.dp
        ) {
            TextButton(onClick = onBack) {
                Text(
                    text = stringResource(R.string.preview_action_back),
                    color = SavedPreviewActive,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Text(
            text = stringResource(R.string.preview_title),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SavedPreviewTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SavedPreviewContent(
    image: SavedImage,
    isActivePage: Boolean,
    onImageZoomChanged: (Boolean) -> Unit,
    deleteState: SavedImageDeleteState,
    shareState: SavedImageShareState,
    openState: SavedImageOpenState,
    onDeleteImage: (SavedImage) -> Unit,
    onShareImage: (SavedImage) -> Unit,
    onOpenImage: (SavedImage) -> Unit,
    modifier: Modifier = Modifier
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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SavedPreviewMediaCard(
            image = image,
            isActivePage = isActivePage,
            onImageZoomChanged = onImageZoomChanged,
            failedToLoad = failedToLoad,
            onImageLoadFailed = { failedToLoad = true }
        )

        SavedInfoChips(image = image)

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

@Composable
private fun SavedPreviewMediaCard(
    image: SavedImage,
    isActivePage: Boolean,
    onImageZoomChanged: (Boolean) -> Unit,
    failedToLoad: Boolean,
    onImageLoadFailed: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), clip = false),
        colors = CardDefaults.cardColors(containerColor = SavedPreviewSurface),
        border = BorderStroke(1.dp, SavedPreviewBorder),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 360.dp, max = 660.dp)
                .padding(8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SavedPreviewMediaBackground),
            contentAlignment = Alignment.Center
        ) {
            if (image.mediaType == SavedMediaType.Video) {
                if (isActivePage) {
                    VideoPlayerPreview(
                        uri = image.uri,
                        modifier = Modifier.fillMaxSize(),
                        showPlaybackStatus = true,
                        errorMessage = stringResource(R.string.preview_video_load_error_body)
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_arrow),
                        contentDescription = stringResource(R.string.preview_video_inactive_description),
                        modifier = Modifier.size(46.dp),
                        tint = SavedPreviewBody
                    )
                }
            } else if (failedToLoad) {
                Text(
                    text = stringResource(R.string.preview_load_error_body),
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = SavedPreviewBody
                )
            } else {
                ZoomableAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(image.uri)
                        .build(),
                    contentDescription = stringResource(R.string.saved_image_card_description),
                    resetKey = "${image.uri}-$isActivePage",
                    onZoomChanged = onImageZoomChanged,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    onError = { onImageLoadFailed() }
                )
            }
        }
    }
}

@Composable
private fun SavedInfoChips(image: SavedImage) {
    val formatter = remember { StatusImagePresentationFormatter() }
    val formattedDate = image.dateAddedMillis?.formatDate()
    val formatValue = formatter.formatValue(image.mimeType)
    val sizeValue = formatter.sizeValue(image.sizeBytes)
    val typeText = stringResource(
        if (image.mediaType == SavedMediaType.Video) R.string.preview_video_status_label else R.string.preview_image_status_label
    )
    val originText = stringResource(image.origin.labelRes())

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SavedInfoChip(
                text = typeText,
                iconRes = if (image.mediaType == SavedMediaType.Video) R.drawable.ic_video else R.drawable.ic_image
            )
            SavedInfoChip(
                text = originText,
                iconRes = if (image.origin == SavedMediaOrigin.VideoPart) R.drawable.ic_video else R.drawable.ic_nav_saved
            )
        }
        formatValue?.let { value ->
            SavedInfoChip(text = stringResource(R.string.preview_info_chip, stringResource(R.string.status_file_format_label), value))
        }
        formattedDate?.let { date ->
            SavedInfoChip(text = stringResource(R.string.saved_detail_saved_date_value, date))
        }
        sizeValue?.let { size ->
            SavedInfoChip(text = stringResource(R.string.preview_info_chip, stringResource(R.string.saved_detail_size_label), size))
        }
    }
}

@Composable
private fun SavedInfoChip(
    text: String,
    iconRes: Int? = null
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = SavedPreviewSurface,
        contentColor = SavedPreviewTitle,
        border = BorderStroke(1.dp, SavedPreviewBorder),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            iconRes?.let { res ->
                Icon(
                    painter = painterResource(res),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = SavedPreviewBlue
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = SavedPreviewTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailsToggleButton(
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SavedPreviewSurface,
        contentColor = SavedPreviewActive,
        border = BorderStroke(1.dp, SavedPreviewBorder),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = SavedPreviewActive
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(if (expanded) R.string.saved_action_hide_details else R.string.saved_action_view_details),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = SavedPreviewActive,
                maxLines = 1
            )
        }
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
        GradientActionButton(
            text = stringResource(R.string.saved_action_share_media),
            iconRes = R.drawable.ic_share,
            onClick = onShareImage,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        )

        SecondaryActionButton(
            text = stringResource(R.string.saved_action_open_with),
            onClick = onOpenImage,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        )

        DeleteActionButton(
            text = stringResource(R.string.saved_action_delete),
            iconRes = R.drawable.ic_delete,
            onClick = onDeleteImage,
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
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
    Row(
        modifier = modifier
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) SavedPreviewGradient else Brush.horizontalGradient(listOf(SavedPreviewBorder, SavedPreviewBorder)))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.White
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )
    }
}

@Composable
private fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) SavedPreviewSurfaceSoft else SavedPreviewSurfaceSoft.copy(alpha = 0.54f))
            .border(1.dp, if (enabled) SavedPreviewActive.copy(alpha = 0.72f) else SavedPreviewBorder, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (enabled) SavedPreviewActive else SavedPreviewBody,
            maxLines = 1
        )
    }
}

@Composable
private fun DeleteActionButton(
    text: String,
    iconRes: Int,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val deleteColor = SavedPreviewDanger
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SavedPreviewDangerSoft,
        contentColor = deleteColor,
        border = BorderStroke(1.dp, deleteColor.copy(alpha = 0.58f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = deleteColor
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = deleteColor,
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
        colors = CardDefaults.cardColors(containerColor = SavedPreviewSurface),
        border = BorderStroke(1.dp, SavedPreviewBorder),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
            color = SavedPreviewBody,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.62f),
            style = MaterialTheme.typography.bodyMedium,
            color = SavedPreviewTitle,
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
        colors = CardDefaults.cardColors(containerColor = SavedPreviewSurface),
        border = BorderStroke(1.dp, SavedPreviewBorder),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SavedPreviewTitle
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = SavedPreviewBody
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
        colors = CardDefaults.cardColors(containerColor = SavedPreviewSurface),
        border = BorderStroke(1.dp, SavedPreviewBorder),
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
                color = SavedPreviewBody
            )
            if (canDismiss) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.saved_delete_dialog_cancel), color = SavedPreviewActive)
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
