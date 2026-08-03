package com.guardaestados.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.guardaestados.data.folder.FolderSelectionState
import com.guardaestados.ui.screens.HomeScreen
import com.guardaestados.ui.screens.SettingsScreen
import com.guardaestados.ui.screens.StatesScreen

@Composable
fun AppNavigation(
    folderSelectionState: FolderSelectionState,
    onSelectFolder: () -> Unit
) {
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
                StatesScreen(folderSelectionState = folderSelectionState)
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
