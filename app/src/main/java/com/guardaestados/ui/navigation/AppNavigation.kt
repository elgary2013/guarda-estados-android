package com.guardaestados.ui.navigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.guardaestados.domain.status.StatusGalleryState
import com.guardaestados.ui.save.SaveStatusImageViewModel
import com.guardaestados.ui.settings.SettingsResetState
import com.guardaestados.ui.save.SaveStatusImageViewModelFactory
import com.guardaestados.ui.saved.SavedImageDeleteState
import com.guardaestados.ui.saved.SavedImagePreviewResolver
import com.guardaestados.ui.saved.SavedImagesViewModel
import com.guardaestados.ui.saved.SavedImagesViewModelFactory
import com.guardaestados.ui.share.ShareStatusImageViewModel
import com.guardaestados.ui.share.ShareStatusImageViewModelFactory
import com.guardaestados.ui.screens.HomeScreen
import com.guardaestados.ui.screens.ImagePreviewScreen
import com.guardaestados.ui.screens.SavedImagePreviewScreen
import com.guardaestados.ui.screens.SavedImagesScreen
import com.guardaestados.ui.screens.SettingsScreen
import com.guardaestados.ui.screens.StatesScreen
import com.guardaestados.ui.screens.VideoSplitterScreen
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
    onSelectFolder: () -> Unit,
    onSelectSaveDestination: () -> Unit,
    onUseDefaultSaveDestination: () -> Unit,
    onThemePreferenceSelected: (AppThemePreference) -> Unit,
    resetState: SettingsResetState,
    onResetSettings: () -> Unit,
    onResetMessageDismissed: () -> Unit
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
    val shareStatusImageState by shareStatusImageViewModel.uiState.collectAsState()
    val savedImagesState by savedImagesViewModel.uiState.collectAsState()
    val deleteSavedImageState by savedImagesViewModel.deleteState.collectAsState()
    val shareSavedImageState by savedImagesViewModel.shareState.collectAsState()
    val openSavedImageState by savedImagesViewModel.openState.collectAsState()
    val videoSplitterState by videoSplitterViewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val routes = listOf(AppRoute.Home, AppRoute.States, AppRoute.Saved, AppRoute.Settings)
    val currentBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentDestination = currentBackStackEntry?.destination
    val previewResolver = remember { StatusImagePreviewResolver() }
    val savedPreviewResolver = remember { SavedImagePreviewResolver() }
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        videoSplitterViewModel.onVideoSelected(uri)
        if (uri != null) {
            navController.navigate(AppRoute.VideoSplitter.route)
        }
    }
    val deleteConfirmationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        savedImagesViewModel.onSystemDeleteConfirmationResult(result.resultCode == Activity.RESULT_OK)
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
        bottomBar = {
            NavigationBar {
                routes.forEach { route ->
                    val selected = currentDestination?.hierarchy?.any { it.route == route.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(route.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(text = stringResource(route.labelRes)) },
                        icon = {
                            route.iconRes?.let { iconRes ->
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = stringResource(route.labelRes)
                                )
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoute.Home.route) {
                HomeScreen(
                    folderSelectionState = folderSelectionState,
                    onSelectFolder = onSelectFolder,
                    onOpenStates = { navController.navigate(AppRoute.States.route) },
                    onOpenVideoSplitter = { navController.navigate(AppRoute.VideoSplitter.route) }
                )
            }
            composable(AppRoute.States.route) {
                StatesScreen(
                    statusGalleryState = statusGalleryState,
                    onRefresh = statusGalleryViewModel::refresh,
                    onImageSelected = { image ->
                        navController.navigate(AppRoute.ImagePreview.createRoute(image.uri.toString()))
                    }
                )
            }
            composable(AppRoute.Saved.route) {
                SavedImagesScreen(
                    savedImagesState = savedImagesState,
                    deleteState = deleteSavedImageState,
                    onRefresh = savedImagesViewModel::refresh,
                    onImageSelected = { image ->
                        navController.navigate(AppRoute.SavedImagePreview.createRoute(image.uri.toString()))
                    },
                    onDeleteImage = savedImagesViewModel::delete,
                    onDeleteMessageDismissed = savedImagesViewModel::clearDeleteMessage
                )
            }
            composable(AppRoute.VideoSplitter.route) {
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
            composable(AppRoute.Settings.route) {
                SettingsScreen(
                    folderSelectionState = folderSelectionState,
                    themePreference = themePreference,
                    saveDestinationState = saveDestinationState,
                    appVersion = appVersion,
                    onSelectFolder = onSelectFolder,
                    onSelectSaveDestination = onSelectSaveDestination,
                    onUseDefaultSaveDestination = onUseDefaultSaveDestination,
                    onThemePreferenceSelected = onThemePreferenceSelected,
                    resetState = resetState,
                    onResetSettings = onResetSettings,
                    onResetMessageDismissed = onResetMessageDismissed
                )
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
                ImagePreviewScreen(
                    previewState = previewResolver.resolve(statusGalleryState, imageUri),
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
                SavedImagePreviewScreen(
                    previewState = savedPreviewResolver.resolve(savedImagesState, imageUri),
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
