package com.guardaestados.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guardaestados.data.folder.FolderSelectionRepository
import com.guardaestados.data.folder.FolderSelectionState
import com.guardaestados.data.folder.takeSaveDestinationFolderPermission
import com.guardaestados.data.folder.takeSelectedFolderPermission
import com.guardaestados.ui.navigation.AppNavigation
import com.guardaestados.ui.settings.SettingsViewModel
import com.guardaestados.ui.settings.SettingsViewModelFactory
import com.guardaestados.ui.theme.GuardaEstadosTheme
import kotlinx.coroutines.launch

@Composable
fun GuardaEstadosApp() {
    val context = LocalContext.current
    val repository = remember(context) { FolderSelectionRepository(context) }
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = remember(context) { SettingsViewModelFactory(context) }
    )
    val folderSelectionState by repository.selectionState.collectAsState(
        initial = FolderSelectionState.Loading
    )
    val themePreference by settingsViewModel.themePreference.collectAsState()
    val resetState by settingsViewModel.resetState.collectAsState()
    val saveDestinationState by settingsViewModel.saveDestinationState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.takeSelectedFolderPermission(uri)
            coroutineScope.launch {
                repository.saveSelectedFolder(uri)
            }
        }
    }
    val saveDestinationPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.takeSaveDestinationFolderPermission(uri)
            settingsViewModel.selectSaveDestination(uri)
        }
    }
    val appVersion = remember(context) { context.installedVersionName() }
    GuardaEstadosTheme(themeMode = themePreference.toThemeMode()) {
        AppNavigation(
            folderSelectionState = folderSelectionState,
            themePreference = themePreference,
            saveDestinationState = saveDestinationState,
            appVersion = appVersion,
            onSelectFolder = { folderPicker.launch(null) },
            onSelectSaveDestination = { saveDestinationPicker.launch(null) },
            onUseDefaultSaveDestination = settingsViewModel::useDefaultSaveDestination,
            onThemePreferenceSelected = settingsViewModel::selectTheme,
            resetState = resetState,
            onResetSettings = settingsViewModel::resetSettings,
            onResetMessageDismissed = settingsViewModel::clearResetMessage
        )
    }
}

private fun Context.installedVersionName(): String {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    return packageInfo.versionName.orEmpty()
}
