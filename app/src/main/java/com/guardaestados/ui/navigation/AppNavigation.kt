package com.guardaestados.ui.navigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.guardaestados.data.folder.FolderSelectionState
import com.guardaestados.data.settings.AppThemePreference
import com.guardaestados.data.settings.SaveDestinationState
import com.guardaestados.domain.saved.SavedImage
import com.guardaestados.domain.status.StatusGalleryState
import com.guardaestados.domain.status.StatusImage
import com.guardaestados.ui.save.SaveStatusImageViewModel
import com.guardaestados.ui.save.SaveStatusImageViewModelFactory
import com.guardaestados.ui.saved.SavedImageDeleteState
import com.guardaestados.ui.saved.SavedImagePreviewResolver
import com.guardaestados.ui.saved.SavedImagesViewModel
import com.guardaestados.ui.saved.SavedImagesViewModelFactory
import com.guardaestados.ui.settings.SettingsResetState
import com.guardaestados.ui.share.ShareStatusImageViewModel
import com.guardaestados.ui.share.ShareStatusImageViewModelFactory
import com.guardaestados.ui.screens.HomeScreen
import com.guardaestados.ui.screens.ImagePreviewScreen
import com.guardaestados.ui.screens.SavedImagePreviewScreen
import com.guardaestados.ui.screens.SavedImagesScreen
import com.guardaestados.ui.screens.SettingsScreen
import com.guardaestados.ui.screens.StatesScreen
import com.guardaestados.ui.screens.VideoSplitterScreen
import com.guardaestados.ui.theme.LocalGuardaEstadosColors
import com.guardaestados.ui.status.StatusGalleryViewModel
import com.guardaestados.ui.status.StatusGalleryViewModelFactory
import com.guardaestados.ui.status.StatusImagePreviewResolver
import com.guardaestados.ui.video.VideoSplitterViewModel
import com.guardaestados.ui.video.VideoSplitterViewModelFactory

@Composable
fun AppNavigation(
    folderSelectionState: FolderSelectionState,
    themePreference: AppThemePreference,
    saveDestinationState: SaveDestinationState,
    appVersion: String,
    homeBackgroundUri: String?,
    onSelectRecommendedFolder: () -> Unit,
    onSelectFolder: () -> Unit,
    onSelectHomeBackground: () -> Unit,
    onClearHomeBackground: () -> Unit,
    onSelectSaveDestination: () -> Unit,
    onUseDefaultSaveDestination: () -> Unit,
    onThemePreferenceSelected: (AppThemePreference) -> Unit,
    resetState: SettingsResetState,
    onResetSettings: () -> Unit,
    onResetMessageDismissed: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onHomePhotoSystemBarsStateChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val statusGalleryViewModel: StatusGalleryViewModel = viewModel(
        factory = remember(context) { StatusGalleryViewModelFactory(context) }
    )
    val saveStatusImageViewModel: SaveStatusImageViewModel = viewModel(
        factory = remember(context) { SaveStatusImageViewModelFactory(context) }
    )
    val shareStatusImageViewModel: ShareStatusImageViewModel = viewModel(
        factory = remember(context) { ShareStatusImageViewModelFactory(context) }
    )
    val savedImagesViewModel: SavedImagesViewModel = viewModel(
        factory = remember(context) { SavedImagesViewModelFactory(context) }
    )
    val videoSplitterViewModel: VideoSplitterViewModel = viewModel(
        factory = remember(context) { VideoSplitterViewModelFactory(context) }
    )
    val statusGalleryState by statusGalleryViewModel.uiState.collectAsState(
        initial = StatusGalleryState.Loading
    )
    val saveStatusImageState by saveStatusImageViewModel.uiState.collectAsState()
    val multiSaveStatusImageState by saveStatusImageViewModel.multiSaveState.collectAsState()
    val shareStatusImageState by shareStatusImageViewModel.uiState.collectAsState()
    val savedImagesState by savedImagesViewModel.uiState.collectAsState()
    val savedImagesRefreshing by savedImagesViewModel.isRefreshing.collectAsState()
    val deleteSavedImageState by savedImagesViewModel.deleteState.collectAsState()
    val shareSavedImageState by savedImagesViewModel.shareState.collectAsState()
    val multiShareSavedImageState by savedImagesViewModel.multiShareState.collectAsState()
    val multiDeleteSavedImageState by savedImagesViewModel.multiDeleteState.collectAsState()
    val openSavedImageState by savedImagesViewModel.openState.collectAsState()
    val videoSplitterState by videoSplitterViewModel.uiState.collectAsState()
    var selectedStatusPreviewItems by remember { mutableStateOf<List<StatusImage>>(emptyList()) }
    var selectedStatusPreviewInitialIndex by remember { mutableStateOf(0) }
    var selectedSavedPreviewItems by remember { mutableStateOf<List<SavedImage>>(emptyList()) }
    var selectedSavedPreviewInitialIndex by remember { mutableStateOf(0) }
    val navController = rememberNavController()
    val routes = listOf(
        AppRoute.Home,
        AppRoute.States,
        AppRoute.VideoSplitter,
        AppRoute.Saved,
        AppRoute.Settings
    )
    val currentBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentDestination = currentBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showBottomBar = routes.any { route -> currentDestination?.hierarchy?.any { it.route == route.route } == true }
    val glassOnHomePhoto = currentRoute == AppRoute.Home.route && homeBackgroundUri != null
    val routesThatResetVideoSplitter = remember {
        setOf(
            AppRoute.Home.route,
            AppRoute.States.route,
            AppRoute.Saved.route,
            AppRoute.Settings.route
        )
    }
    val previewResolver = remember { StatusImagePreviewResolver() }
    val savedPreviewResolver = remember { SavedImagePreviewResolver() }
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        videoSplitterViewModel.onVideoSelected(uri)
        if (uri != null) {
            navController.navigate(AppRoute.VideoSplitter.route) {
                launchSingleTop = true
            }
        }
    }
    val deleteConfirmationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        savedImagesViewModel.onSystemDeleteConfirmationResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(glassOnHomePhoto) {
        onHomePhotoSystemBarsStateChanged(glassOnHomePhoto)
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute in routesThatResetVideoSplitter) {
            videoSplitterViewModel.resetTemporaryStateIfIdle()
        }
    }

    LaunchedEffect(deleteSavedImageState) {
        val confirmationState = deleteSavedImageState as? SavedImageDeleteState.NeedsSystemConfirmation
            ?: return@LaunchedEffect
        try {
            deleteConfirmationLauncher.launch(
                IntentSenderRequest.Builder(confirmationState.intentSender).build()
            )
            savedImagesViewModel.onSystemDeleteConfirmationLaunched()
        } catch (exception: Exception) {
            savedImagesViewModel.onSystemDeleteConfirmationResult(confirmed = false)
        }
    }

    Scaffold(
        contentWindowInsets = if (glassOnHomePhoto) {
            WindowInsets(0.dp)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        bottomBar = {
            if (showBottomBar) {
                SoloEstadosBottomBar(
                    routes = routes,
                    currentRoute = currentRoute,
                    glassOnPhoto = glassOnHomePhoto,
                    onRouteSelected = { route ->
                        navController.navigate(route.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Home.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(AppRoute.Home.route) {
                Box(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        homeBackgroundUri = homeBackgroundUri,
                        contentPadding = innerPadding
                    )
                }
            }
            composable(AppRoute.States.route) {
                PaddedNavigationContent(innerPadding) {
                    StatesScreen(
                        statusGalleryState = statusGalleryState,
                        multiSaveState = multiSaveStatusImageState,
                        onRefresh = statusGalleryViewModel::refresh,
                        onSaveSelected = saveStatusImageViewModel::saveAll,
                        onMultiSaveMessageShown = saveStatusImageViewModel::clearMultiSaveResult,
                        onImageSelected = { images, initialIndex ->
                            val image = images.getOrNull(initialIndex) ?: return@StatesScreen
                            selectedStatusPreviewItems = images
                            selectedStatusPreviewInitialIndex = initialIndex
                            navController.navigate(AppRoute.ImagePreview.createRoute(image.uri.toString())) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
            composable(AppRoute.Saved.route) {
                PaddedNavigationContent(innerPadding) {
                    SavedImagesScreen(
                        savedImagesState = savedImagesState,
                        deleteState = deleteSavedImageState,
                        multiShareState = multiShareSavedImageState,
                        multiDeleteState = multiDeleteSavedImageState,
                        isRefreshing = savedImagesRefreshing,
                        onRefresh = savedImagesViewModel::refresh,
                        onImageSelected = { images, initialIndex ->
                            val image = images.getOrNull(initialIndex) ?: return@SavedImagesScreen
                            selectedSavedPreviewItems = images
                            selectedSavedPreviewInitialIndex = initialIndex
                            savedImagesViewModel.selectForPreview(image)
                            navController.navigate(AppRoute.SavedImagePreview.createRoute(image.uri.toString())) {
                                launchSingleTop = true
                            }
                        },
                        onDeleteImage = savedImagesViewModel::delete,
                        onShareSelected = savedImagesViewModel::shareAll,
                        onDeleteSelected = savedImagesViewModel::deleteAll,
                        onDeleteMessageDismissed = savedImagesViewModel::clearDeleteMessage,
                        onMultiShareMessageDismissed = savedImagesViewModel::clearMultiShareMessage,
                        onMultiDeleteMessageDismissed = savedImagesViewModel::clearMultiDeleteMessage
                    )
                }
            }
            composable(AppRoute.VideoSplitter.route) {
                PaddedNavigationContent(innerPadding) {
                    VideoSplitterScreen(
                        uiState = videoSplitterState,
                        onPickVideo = { videoPickerLauncher.launch("video/*") },
                        onPartDurationSelected = videoSplitterViewModel::selectPartDuration,
                        onCreateParts = videoSplitterViewModel::createParts,
                        onCancelProcessing = videoSplitterViewModel::cancelProcessing,
                        onPreviewOriginal = videoSplitterViewModel::previewOriginal,
                        onPreviewPart = videoSplitterViewModel::previewPart,
                        onSharePart = videoSplitterViewModel::sharePart,
                        onShareAllParts = videoSplitterViewModel::shareAllParts,
                        onClearMessage = videoSplitterViewModel::clearMessage,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(AppRoute.Settings.route) {
                PaddedNavigationContent(innerPadding) {
                    SettingsScreen(
                        folderSelectionState = folderSelectionState,
                        themePreference = themePreference,
                        saveDestinationState = saveDestinationState,
                        appVersion = appVersion,
                        homeBackgroundUri = homeBackgroundUri,
                        onSelectRecommendedFolder = onSelectRecommendedFolder,
                        onSelectFolder = onSelectFolder,
                        onSelectHomeBackground = onSelectHomeBackground,
                        onClearHomeBackground = onClearHomeBackground,
                        onSelectSaveDestination = onSelectSaveDestination,
                        onUseDefaultSaveDestination = onUseDefaultSaveDestination,
                        onThemePreferenceSelected = onThemePreferenceSelected,
                        resetState = resetState,
                        onResetSettings = onResetSettings,
                        onResetMessageDismissed = onResetMessageDismissed,
                        onOpenPrivacyPolicy = onOpenPrivacyPolicy
                    )
                }
            }
            composable(
                route = AppRoute.ImagePreview.route,
                arguments = listOf(
                    navArgument(AppRoute.ImagePreview.ImageUriArgument) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val imageUri = backStackEntry.arguments?.getString(AppRoute.ImagePreview.ImageUriArgument)
                val previewItems = selectedStatusPreviewItems.forStatusPreviewRoute(imageUri)
                ImagePreviewScreen(
                    previewState = previewResolver.resolve(statusGalleryState, imageUri),
                    previewItems = previewItems,
                    initialIndex = previewItems.initialStatusPreviewIndex(imageUri, selectedStatusPreviewInitialIndex),
                    saveState = saveStatusImageState,
                    shareState = shareStatusImageState,
                    onSaveImage = saveStatusImageViewModel::save,
                    onShareImage = shareStatusImageViewModel::share,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = AppRoute.SavedImagePreview.route,
                arguments = listOf(
                    navArgument(AppRoute.SavedImagePreview.SavedImageUriArgument) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val imageUri = backStackEntry.arguments?.getString(
                    AppRoute.SavedImagePreview.SavedImageUriArgument
                )
                val previewItems = selectedSavedPreviewItems.forSavedPreviewRoute(imageUri)
                SavedImagePreviewScreen(
                    previewState = savedPreviewResolver.resolve(savedImagesState, imageUri),
                    previewItems = previewItems,
                    initialIndex = previewItems.initialSavedPreviewIndex(imageUri, selectedSavedPreviewInitialIndex),
                    deleteState = deleteSavedImageState,
                    shareState = shareSavedImageState,
                    openState = openSavedImageState,
                    onDeleteImage = savedImagesViewModel::delete,
                    onShareImage = savedImagesViewModel::share,
                    onOpenImage = savedImagesViewModel::open,
                    onShareMessageDismissed = savedImagesViewModel::clearShareMessage,
                    onOpenMessageDismissed = savedImagesViewModel::clearOpenMessage,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun PaddedNavigationContent(
    innerPadding: PaddingValues,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        content()
    }
}

private fun List<StatusImage>.forStatusPreviewRoute(routeUri: String?): List<StatusImage> {
    if (routeUri.isNullOrBlank()) return emptyList()
    return takeIf { images -> images.any { image -> image.uri.toString() == routeUri } }.orEmpty()
}

private fun List<SavedImage>.forSavedPreviewRoute(routeUri: String?): List<SavedImage> {
    if (routeUri.isNullOrBlank()) return emptyList()
    return takeIf { images -> images.any { image -> image.uri.toString() == routeUri } }.orEmpty()
}

private fun List<StatusImage>.initialStatusPreviewIndex(routeUri: String?, fallbackIndex: Int): Int {
    if (isEmpty()) return 0
    val routeIndex = indexOfFirst { image -> image.uri.toString() == routeUri }
    return if (routeIndex >= 0) routeIndex else fallbackIndex.coerceIn(indices)
}

private fun List<SavedImage>.initialSavedPreviewIndex(routeUri: String?, fallbackIndex: Int): Int {
    if (isEmpty()) return 0
    val routeIndex = indexOfFirst { image -> image.uri.toString() == routeUri }
    return if (routeIndex >= 0) routeIndex else fallbackIndex.coerceIn(indices)
}

@Composable
private fun SoloEstadosBottomBar(
    routes: List<AppRoute>,
    currentRoute: String?,
    glassOnPhoto: Boolean,
    onRouteSelected: (AppRoute) -> Unit
) {
    val colors = LocalGuardaEstadosColors.current
    val containerColor = if (glassOnPhoto) Color(0xB8031519) else colors.surface.copy(alpha = 0.94f)
    val borderColor = if (glassOnPhoto) Color.White.copy(alpha = 0.18f) else colors.border
    val contentColor = if (glassOnPhoto) Color.White.copy(alpha = 0.78f) else colors.body
    val barMinHeight = 76.dp
    val barVerticalPadding = 6.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (glassOnPhoto) 0.dp else 12.dp)
            .background(containerColor)
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(0.5.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = barMinHeight)
                .padding(horizontal = 6.dp, vertical = barVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            routes.forEach { route ->
                BottomBarItem(
                    route = route,
                    selected = currentRoute == route.route,
                    onClick = { onRouteSelected(route) },
                    glassOnPhoto = glassOnPhoto
                )
            }
        }
    }
}

@Composable
private fun RowScope.BottomBarItem(
    route: AppRoute,
    selected: Boolean,
    onClick: () -> Unit,
    glassOnPhoto: Boolean
) {
    val colors = LocalGuardaEstadosColors.current
    val activeColor = colors.active
    val inactiveColor = if (glassOnPhoto) Color.White.copy(alpha = 0.74f) else colors.body
    val itemShape = RoundedCornerShape(18.dp)
    val itemContentColor = if (selected) activeColor else inactiveColor
    val isCentralAction = route == AppRoute.VideoSplitter

    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = if (isCentralAction) 64.dp else 58.dp)
            .background(
                color = if (selected && !isCentralAction && !glassOnPhoto) colors.surfaceSoft else Color.Transparent,
                shape = itemShape
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isCentralAction) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(50),
                color = if (selected) activeColor.copy(alpha = 0.18f) else Color.Transparent,
                contentColor = itemContentColor,
                border = BorderStroke(
                    width = if (selected || glassOnPhoto) 1.5.dp else 1.dp,
                    color = if (selected) activeColor else inactiveColor.copy(alpha = 0.58f)
                ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    route.iconRes?.let { iconRes ->
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = itemContentColor
                        )
                    }
                }
            }
        } else {
            route.iconRes?.let { iconRes ->
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = itemContentColor
                )
            }
        }
        Text(
            text = stringResource(route.labelRes),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = itemContentColor,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center
        )
    }
}
