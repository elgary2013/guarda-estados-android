package com.guardaestados.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.guardaestados.domain.status.StatusGalleryState
import com.guardaestados.ui.save.SaveStatusImageViewModel
import com.guardaestados.ui.save.SaveStatusImageViewModelFactory
import com.guardaestados.ui.share.ShareStatusImageViewModel
import com.guardaestados.ui.share.ShareStatusImageViewModelFactory
import com.guardaestados.ui.screens.HomeScreen
import com.guardaestados.ui.screens.ImagePreviewScreen
import com.guardaestados.ui.screens.SettingsScreen
import com.guardaestados.ui.screens.StatesScreen
import com.guardaestados.ui.status.StatusGalleryViewModel
import com.guardaestados.ui.status.StatusGalleryViewModelFactory
import com.guardaestados.ui.status.StatusImagePreviewResolver

@Composable
fun AppNavigation(
    folderSelectionState: FolderSelectionState,
    onSelectFolder: () -> Unit
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
    val statusGalleryState by statusGalleryViewModel.uiState.collectAsState(
        initial = StatusGalleryState.Loading
    )
    val saveStatusImageState by saveStatusImageViewModel.uiState.collectAsState()
    val shareStatusImageState by shareStatusImageViewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val routes = listOf(AppRoute.Home, AppRoute.States, AppRoute.Settings)
    val currentBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentDestination = currentBackStackEntry?.destination
    val previewResolver = remember { StatusImagePreviewResolver() }

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
                    onOpenStates = { navController.navigate(AppRoute.States.route) }
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
            composable(AppRoute.Settings.route) {
                SettingsScreen(
                    folderSelectionState = folderSelectionState,
                    onSelectFolder = onSelectFolder
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
        }
    }
}
