package com.guardaestados.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.guardaestados.data.folder.FolderSelectionRepository
import com.guardaestados.data.folder.FolderSelectionState
import com.guardaestados.data.folder.takeSelectedFolderPermission
import com.guardaestados.ui.navigation.AppNavigation
import com.guardaestados.ui.theme.GuardaEstadosTheme
import kotlinx.coroutines.launch

@Composable
fun GuardaEstadosApp() {
    val context = LocalContext.current
    val repository = remember(context) { FolderSelectionRepository(context) }
    val folderSelectionState by repository.selectionState.collectAsState(
        initial = FolderSelectionState.Loading
    )
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

    GuardaEstadosTheme {
        AppNavigation(
            folderSelectionState = folderSelectionState,
            onSelectFolder = { folderPicker.launch(null) }
        )
    }
}
