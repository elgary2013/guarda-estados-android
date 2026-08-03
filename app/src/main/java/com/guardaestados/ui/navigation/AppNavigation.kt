package com.guardaestados.ui.navigation

import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.guardaestados.data.folder.FolderSelectionState
import com.guardaestados.domain.status.StatusGalleryState
import com.guardaestados.ui.screens.HomeScreen
import com.guardaestados.ui.screens.SettingsScreen
import com.guardaestados.ui.screens.StatesScreen
import com.guardaestados.ui.status.StatusGalleryViewModel
import com.guardaestados.ui.status.StatusGalleryViewModelFactory

@Composable
fun AppNavigation(
    folderSelectionState: FolderSelectionState,
    onSelectFolder: () -> Unit
) {
    val context = LocalContext.current
    val statusGalleryViewModel: StatusGalleryViewModel = viewModel(
        factory = remember(context) { StatusGalleryViewModelFactory(context) }
    )
    val statusGalleryState by statusGalleryViewModel.uiState.collectAsState(
        initial = StatusGalleryState.Loading
    )
    val navController = rememberNavController()
    val routes = listOf(AppRoute.Home, AppRoute.States, AppRoute.Settings)
    val currentBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentDestination = currentBackStackEntry?.destination

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
                        icon = { Text(text = stringResource(route.labelRes).take(1)) }
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
                    onRefresh = statusGalleryViewModel::refresh
                )
            }
            composable(AppRoute.Settings.route) {
                SettingsScreen(
                    folderSelectionState = folderSelectionState,
                    onSelectFolder = onSelectFolder
                )
            }
        }
    }
}
