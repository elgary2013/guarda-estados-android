package com.guardaestados.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guardaestados.data.folder.FolderSelectionRepository
import com.guardaestados.data.folder.FolderSelectionState
import com.guardaestados.data.folder.takeSelectedFolderPermission
import com.guardaestados.data.settings.AppThemePreference
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
    val appVersion = remember(context) { context.installedVersionName() }
    val useDarkTheme = when (themePreference) {
        AppThemePreference.System -> isSystemInDarkTheme()
        AppThemePreference.Light -> false
        AppThemePreference.Dark -> true
    }

    GuardaEstadosTheme(darkTheme = useDarkTheme) {
        AppNavigation(
            folderSelectionState = folderSelectionState,
            themePreference = themePreference,
            appVersion = appVersion,
            onSelectFolder = { folderPicker.launch(null) },
            onThemePreferenceSelected = settingsViewModel::selectTheme
        )
    }
}

private fun Context.installedVersionName(): String {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    return packageInfo.versionName.orEmpty()
}
