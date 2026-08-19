package com.guardaestados.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
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
import com.guardaestados.domain.status.StatusImage
import com.guardaestados.domain.status.StatusMediaType
import com.guardaestados.ui.components.ZoomableAsyncImage
import com.guardaestados.ui.save.SaveStatusImageUiState
import com.guardaestados.ui.share.ShareStatusImageUiState
import com.guardaestados.ui.status.StatusImagePresentationFormatter
import com.guardaestados.ui.status.StatusImagePreviewState
import com.guardaestados.ui.video.VideoPlayerPreview
import java.text.DateFormat
import java.util.Date

private val PreviewBackground: Color
    @Composable get() = LocalGuardaEstadosColors.current.background
private val PreviewSurface: Color
    @Composable get() = LocalGuardaEstadosColors.current.surface
private val PreviewTitle: Color
    @Composable get() = LocalGuardaEstadosColors.current.title
private val PreviewBody: Color
    @Composable get() = LocalGuardaEstadosColors.current.body
private val PreviewBorder: Color
    @Composable get() = LocalGuardaEstadosColors.current.border
private val PreviewActive: Color
    @Composable get() = LocalGuardaEstadosColors.current.active
private val PreviewMutedSurface: Color
    @Composable get() = LocalGuardaEstadosColors.current.surfaceStrong
private val PreviewErrorSurface: Color
    @Composable get() = LocalGuardaEstadosColors.current.dangerSoft
private val PreviewErrorBorder: Color
    @Composable get() = LocalGuardaEstadosColors.current.dangerBorder
private val PreviewErrorText: Color
    @Composable get() = LocalGuardaEstadosColors.current.danger
private val PreviewGradient: Brush
    @Composable get() = LocalGuardaEstadosColors.current.primaryGradient
private val ImmersivePreviewBackground = Color(0xFF050607)
private val ImmersiveOverlay = Color.Black.copy(alpha = 0.48f)
private val ImmersiveContent = Color.White
private val ImmersiveMutedContent = Color.White.copy(alpha = 0.72f)
private val ImmersiveSnackbarContainer = Color(0xFFEAF3EF)
private val ImmersiveSnackbarContent = Color(0xFF101615)

@Composable
fun ImagePreviewScreen(
    previewState: StatusImagePreviewState,
    previewItems: List<StatusImage>,
    initialIndex: Int,
    saveState: SaveStatusImageUiState,
    shareState: ShareStatusImageUiState,
    onSaveImage: (StatusImage) -> Unit,
    onShareImage: (StatusImage) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {

    val activeImage = (previewState as? StatusImagePreviewState.Content)?.image
    val useImmersivePreview = activeImage?.mediaType == StatusMediaType.Image ||
        activeImage?.mediaType == StatusMediaType.Video
    val screenBackground = if (useImmersivePreview) ImmersivePreviewBackground else PreviewBackground

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
                PreviewTopBar(onBack = onBack)
            }

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

                is StatusImagePreviewState.Content -> PreviewPagerContent(
                    images = previewItems.ifEmpty { listOf(previewState.image) },
                    initialIndex = initialIndex,
                    immersivePreview = previewState.image.mediaType == StatusMediaType.Image ||
                        previewState.image.mediaType == StatusMediaType.Video,
                    saveState = saveState,
                    shareState = shareState,
                    onSaveImage = onSaveImage,
                    onShareImage = onShareImage,
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.PreviewPagerContent(
    images: List<StatusImage>,
    initialIndex: Int,
    immersivePreview: Boolean,
    saveState: SaveStatusImageUiState,
    shareState: ShareStatusImageUiState,
    onSaveImage: (StatusImage) -> Unit,
    onShareImage: (StatusImage) -> Unit,
    onBack: () -> Unit
) {
    if (images.isEmpty()) {
        PreviewMessageCard(
            title = stringResource(R.string.preview_unavailable_title),
            body = stringResource(R.string.preview_unavailable_body)
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
        var saveSnackbarRequested by remember { mutableStateOf(false) }
        var saveSnackbarForVideo by remember { mutableStateOf(false) }
        var shareSnackbarRequested by remember { mutableStateOf(false) }
        val saveSuccessMessage = stringResource(R.string.preview_snackbar_image_saved)
        val saveVideoSuccessMessage = stringResource(R.string.preview_snackbar_video_saved)
        val saveDuplicateMessage = stringResource(R.string.save_status_duplicate_image)
        val saveDuplicateVideoMessage = stringResource(R.string.save_status_duplicate_video)
        val saveDestinationPermissionLostMessage = stringResource(R.string.save_status_destination_permission_lost)
        val saveDestinationUnavailableMessage = stringResource(R.string.save_status_destination_error)
        val saveErrorMessage = stringResource(R.string.save_status_error)
        val shareNoCompatibleAppMessage = stringResource(R.string.share_status_no_app)
        val shareErrorMessage = stringResource(R.string.share_status_error)

        LaunchedEffect(saveState) {
            if (!saveSnackbarRequested) return@LaunchedEffect
            val message = when (saveState) {
                SaveStatusImageUiState.Idle,
                SaveStatusImageUiState.Saving -> null
                SaveStatusImageUiState.Duplicate -> if (saveSnackbarForVideo) saveDuplicateVideoMessage else saveDuplicateMessage
                SaveStatusImageUiState.DestinationPermissionLost -> saveDestinationPermissionLostMessage
                SaveStatusImageUiState.DestinationUnavailable -> saveDestinationUnavailableMessage
                SaveStatusImageUiState.Error -> saveErrorMessage
                is SaveStatusImageUiState.Success -> if (saveSnackbarForVideo) saveVideoSuccessMessage else saveSuccessMessage
            }
            if (message != null) {
                saveSnackbarRequested = false
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(message)
            }
        }

        LaunchedEffect(shareState) {
            if (!shareSnackbarRequested) return@LaunchedEffect
            val message = when (shareState) {
                ShareStatusImageUiState.Idle,
                ShareStatusImageUiState.Sharing -> null
                ShareStatusImageUiState.ChooserOpened -> {
                    shareSnackbarRequested = false
                    null
                }
                ShareStatusImageUiState.NoCompatibleApp -> shareNoCompatibleAppMessage
                ShareStatusImageUiState.Error -> shareErrorMessage
            }
            if (message != null) {
                shareSnackbarRequested = false
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(message)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(ImmersivePreviewBackground)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = images.size > 1 && !activePageZoomed && !activePageVideoSeeking
            ) { page ->
                val pageImage = images[page]
                if (pageImage.mediaType == StatusMediaType.Video) {
                    ImmersiveVideoPage(
                        image = pageImage,
                        isActivePage = page == pagerState.currentPage,
                        onSeekInteractionChanged = { seeking ->
                            if (page == pagerState.currentPage) {
                                activePageVideoSeeking = seeking
                            }
                        }
                    )
                } else {
                    ImmersiveImagePage(
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

            ImmersiveStatusTopBar(
                currentPage = pagerState.currentPage,
                pageCount = images.size,
                saveState = saveState,
                shareState = shareState,
                onBack = onBack,
                onSaveImage = {
                    saveSnackbarForVideo = images[pagerState.currentPage].mediaType == StatusMediaType.Video
                    saveSnackbarRequested = true
                    onSaveImage(images[pagerState.currentPage])
                },
                onShareImage = {
                    shareSnackbarRequested = true
                    onShareImage(images[pagerState.currentPage])
                },
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
                        containerColor = ImmersiveSnackbarContainer,
                        contentColor = ImmersiveSnackbarContent
                    )
                }
            )
        }
    } else {
        Text(
            text = stringResource(R.string.preview_page_indicator, pagerState.currentPage + 1, images.size),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = PreviewBody
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            userScrollEnabled = images.size > 1 && !activePageZoomed
        ) { page ->
            PreviewContent(
                image = images[page],
                isActivePage = page == pagerState.currentPage,
                onImageZoomChanged = { zoomed ->
                    if (page == pagerState.currentPage) {
                        activePageZoomed = zoomed
                    }
                },
                saveState = saveState,
                shareState = shareState,
                onSaveImage = onSaveImage,
                onShareImage = onShareImage,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
private fun ImmersiveImagePage(
    image: StatusImage,
    isActivePage: Boolean,
    onImageZoomChanged: (Boolean) -> Unit
) {
    var failedToLoad by remember(image.uri) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ImmersivePreviewBackground),
        contentAlignment = Alignment.Center
    ) {
        if (failedToLoad) {
            Text(
                text = stringResource(R.string.preview_load_error_body),
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = ImmersiveMutedContent
            )
        } else {
            ZoomableAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image.uri)
                    .build(),
                contentDescription = stringResource(R.string.preview_image_content_description),
                resetKey = "${image.uri}-$isActivePage",
                onZoomChanged = onImageZoomChanged,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .background(ImmersivePreviewBackground),
                onError = { failedToLoad = true }
            )
        }
    }
}

@Composable
private fun ImmersiveVideoPage(
    image: StatusImage,
    isActivePage: Boolean,
    onSeekInteractionChanged: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ImmersivePreviewBackground),
        contentAlignment = Alignment.Center
    ) {
        if (isActivePage) {
            VideoPlayerPreview(
                uri = image.uri,
                modifier = Modifier
                    .fillMaxSize()
                    .background(ImmersivePreviewBackground),
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
                tint = ImmersiveMutedContent
            )
        }
    }
}

@Composable
private fun ImmersiveStatusTopBar(
    currentPage: Int,
    pageCount: Int,
    saveState: SaveStatusImageUiState,
    shareState: ShareStatusImageUiState,
    onBack: () -> Unit,
    onSaveImage: () -> Unit,
    onShareImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val counterText = stringResource(R.string.preview_page_indicator, currentPage + 1, pageCount)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ImmersiveOverlay)
            .statusBarsPadding()
            .padding(start = 6.dp, top = 6.dp, end = 6.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.preview_action_back),
                tint = ImmersiveContent
            )
        }
        SegmentedProgressIndicator(
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
            color = ImmersiveContent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.preview_more_options),
                    tint = ImmersiveContent
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.preview_action_save_copy)) },
                    enabled = saveState != SaveStatusImageUiState.Saving,
                    onClick = {
                        menuExpanded = false
                        onSaveImage()
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.preview_action_share_short)) },
                    enabled = shareState != ShareStatusImageUiState.Sharing,
                    onClick = {
                        menuExpanded = false
                        onShareImage()
                    }
                )
            }
        }
    }
}

@Composable
private fun SegmentedProgressIndicator(
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
            val color = if (index <= currentPage) ImmersiveContent else ImmersiveContent.copy(alpha = 0.42f)
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
private fun PreviewTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = PreviewSurface,
            contentColor = PreviewActive,
            border = BorderStroke(1.dp, PreviewBorder),
            shadowElevation = 0.dp
        ) {
            TextButton(onClick = onBack) {
                Text(
                    text = stringResource(R.string.preview_action_back),
                    color = PreviewActive,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Text(
            text = stringResource(R.string.preview_title),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PreviewTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PreviewContent(
    image: StatusImage,
    isActivePage: Boolean,
    onImageZoomChanged: (Boolean) -> Unit,
    saveState: SaveStatusImageUiState,
    shareState: ShareStatusImageUiState,
    onSaveImage: (StatusImage) -> Unit,
    onShareImage: (StatusImage) -> Unit,
    modifier: Modifier = Modifier
) {
    var failedToLoad by remember(image.uri) { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PreviewMediaCard(
            image = image,
            isActivePage = isActivePage,
            onImageZoomChanged = onImageZoomChanged,
            failedToLoad = failedToLoad,
            onImageLoadFailed = { failedToLoad = true }
        )

        MediaInfoChips(image = image)

        PreviewActionsCard(
            image = image,
            saveState = saveState,
            shareState = shareState,
            onSaveImage = onSaveImage,
            onShareImage = onShareImage
        )
    }
}

@Composable
private fun PreviewMediaCard(
    image: StatusImage,
    isActivePage: Boolean,
    onImageZoomChanged: (Boolean) -> Unit,
    failedToLoad: Boolean,
    onImageLoadFailed: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), clip = false),
        colors = CardDefaults.cardColors(containerColor = PreviewSurface),
        border = BorderStroke(1.dp, PreviewBorder),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .previewMediaFrame(image)
                .padding(8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (image.mediaType == StatusMediaType.Video) {
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
                        tint = Color.White.copy(alpha = 0.82f)
                    )
                }
            } else if (failedToLoad) {
                Text(
                    text = stringResource(R.string.preview_load_error_body),
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.86f)
                )
            } else {
                ZoomableAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(image.uri)
                        .build(),
                    contentDescription = stringResource(R.string.preview_image_content_description),
                    resetKey = "${image.uri}-$isActivePage",
                    onZoomChanged = onImageZoomChanged,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    onError = { onImageLoadFailed() }
                )
            }
        }
    }
}

private fun Modifier.previewMediaFrame(image: StatusImage): Modifier {
    return if (image.mediaType == StatusMediaType.Image) {
        val width = image.widthPixels?.takeIf { it > 0 }
        val height = image.heightPixels?.takeIf { it > 0 }
        if (width != null && height != null) {
            fillMaxWidth()
                .aspectRatio(width.toFloat() / height.toFloat())
                .heightIn(min = 220.dp, max = 720.dp)
        } else {
            fillMaxWidth().heightIn(min = 420.dp, max = 720.dp)
        }
    } else {
        fillMaxWidth().heightIn(min = 380.dp, max = 660.dp)
    }
}
@Composable
private fun MediaInfoChips(image: StatusImage) {
    val formatter = remember { StatusImagePresentationFormatter() }
    val formattedDate = image.lastModifiedMillis?.formatDate()
    val formatValue = formatter.formatValue(image.mimeType)
    val typeText = stringResource(
        if (image.mediaType == StatusMediaType.Video) R.string.preview_video_status_label else R.string.preview_image_status_label
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoChip(text = typeText, iconRes = if (image.mediaType == StatusMediaType.Video) R.drawable.ic_video else R.drawable.ic_image)
            formatValue?.let { value ->
                InfoChip(text = stringResource(R.string.preview_info_chip, stringResource(R.string.status_file_format_label), value))
            }
        }
        formattedDate?.let { date ->
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoChip(text = stringResource(R.string.preview_media_date, date))
            }
        }
    }
}

@Composable
private fun InfoChip(
    text: String,
    iconRes: Int? = null
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = PreviewSurface,
        contentColor = PreviewTitle,
        border = BorderStroke(1.dp, PreviewBorder),
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
                    tint = PreviewActive
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = PreviewTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PreviewActionsCard(
    image: StatusImage,
    saveState: SaveStatusImageUiState,
    shareState: ShareStatusImageUiState,
    onSaveImage: (StatusImage) -> Unit,
    onShareImage: (StatusImage) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = PreviewSurface),
        border = BorderStroke(1.dp, PreviewBorder),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GradientActionButton(
                    text = stringResource(R.string.preview_action_save_short),
                    iconRes = R.drawable.ic_nav_saved,
                    onClick = { onSaveImage(image) },
                    enabled = saveState != SaveStatusImageUiState.Saving,
                    modifier = Modifier.fillMaxWidth()
                )

                SecondaryActionButton(
                    text = stringResource(R.string.preview_action_share_short),
                    iconRes = R.drawable.ic_share,
                    onClick = { onShareImage(image) },
                    enabled = shareState != ShareStatusImageUiState.Sharing,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SaveStatusMessage(saveState = saveState)
                ShareStatusMessage(shareState = shareState)
                PreviewOriginalNotice()
            }
        }
    }
}

@Composable
private fun PreviewOriginalNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = PreviewMutedSurface,
        border = BorderStroke(1.dp, PreviewBorder),
        contentColor = PreviewBody,
        shadowElevation = 0.dp
    ) {
        Text(
            text = stringResource(R.string.preview_original_unchanged_notice),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = PreviewBody
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
            .background(if (enabled) PreviewGradient else Brush.horizontalGradient(listOf(PreviewBorder, PreviewBorder)))
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
    iconRes: Int,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) PreviewMutedSurface else PreviewMutedSurface.copy(alpha = 0.54f))
            .border(1.dp, if (enabled) PreviewActive.copy(alpha = 0.72f) else PreviewBorder, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) PreviewActive else PreviewBody
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (enabled) PreviewActive else PreviewBody,
            maxLines = 1
        )
    }
}

@Composable
private fun SaveStatusMessage(saveState: SaveStatusImageUiState) {
    StatusText(
        message = when (saveState) {
            SaveStatusImageUiState.Idle -> null
            SaveStatusImageUiState.Saving -> stringResource(R.string.save_status_saving)
            SaveStatusImageUiState.Duplicate -> stringResource(R.string.save_status_duplicate_video)
            SaveStatusImageUiState.DestinationPermissionLost -> stringResource(R.string.save_status_destination_permission_lost)
            SaveStatusImageUiState.DestinationUnavailable -> stringResource(R.string.save_status_destination_error)
            SaveStatusImageUiState.Error -> stringResource(R.string.save_status_error)
            is SaveStatusImageUiState.Success -> stringResource(R.string.save_status_success, saveState.displayName)
        },
        isError = saveState == SaveStatusImageUiState.Error ||
            saveState == SaveStatusImageUiState.DestinationPermissionLost ||
            saveState == SaveStatusImageUiState.DestinationUnavailable
    )
}

@Composable
private fun ShareStatusMessage(shareState: ShareStatusImageUiState) {
    StatusText(
        message = when (shareState) {
            ShareStatusImageUiState.Idle -> null
            ShareStatusImageUiState.Sharing -> stringResource(R.string.share_status_opening)
            ShareStatusImageUiState.ChooserOpened -> stringResource(R.string.share_status_chooser_opened)
            ShareStatusImageUiState.NoCompatibleApp -> stringResource(R.string.share_status_no_app)
            ShareStatusImageUiState.Error -> stringResource(R.string.share_status_error)
        },
        isError = shareState == ShareStatusImageUiState.Error || shareState == ShareStatusImageUiState.NoCompatibleApp
    )
}

@Composable
private fun StatusText(
    message: String?,
    isError: Boolean
) {
    AnimatedVisibility(visible = message != null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = if (isError) PreviewErrorSurface else PreviewMutedSurface,
            border = BorderStroke(1.dp, if (isError) PreviewErrorBorder else PreviewBorder)
        ) {
            Text(
                text = message.orEmpty(),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) PreviewErrorText else PreviewBody
            )
        }
    }
}

@Composable
private fun PreviewMessageCard(
    title: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PreviewSurface),
        border = BorderStroke(1.dp, PreviewBorder),
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
                color = PreviewTitle
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = PreviewBody
            )
        }
    }
}

private fun Long.formatDate(): String {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(this))
}
