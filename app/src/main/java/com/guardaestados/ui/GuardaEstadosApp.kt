package com.guardaestados.ui

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guardaestados.R
import com.guardaestados.data.folder.FolderSelectionRepository
import com.guardaestados.data.folder.FolderSelectionState
import com.guardaestados.data.folder.takeSaveDestinationFolderPermission
import com.guardaestados.data.folder.takeSelectedFolderPermission
import com.guardaestados.ui.navigation.AppNavigation
import com.guardaestados.ui.settings.HomeBackgroundNotice
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
    val homeBackgroundUri by settingsViewModel.homeBackgroundUri.collectAsState()
    val includedHomeBackground by settingsViewModel.includedHomeBackground.collectAsState()
    val homeBackgroundNotice by settingsViewModel.homeBackgroundNotice.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        if (uri.isRecommendedStatusesParentFolder()) {
            Toast.makeText(context, R.string.folder_media_selection_rejected, Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        context.takeSelectedFolderPermission(uri)
        coroutineScope.launch {
            repository.saveSelectedFolder(uri)
        }
    }
    val homeBackgroundPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            settingsViewModel.selectHomeBackground(uri)
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
    val systemDarkTheme = isSystemInDarkTheme()
    var drawHomePhotoBehindSystemBars by remember { mutableStateOf(false) }

    LaunchedEffect(homeBackgroundNotice) {
        if (homeBackgroundNotice == HomeBackgroundNotice.PermissionLost) {
            Toast.makeText(context, R.string.home_background_permission_lost, Toast.LENGTH_SHORT).show()
            settingsViewModel.clearHomeBackgroundNotice()
        }
    }

    GuardaEstadosTheme(
        themeMode = themePreference.toThemeMode(systemDarkTheme),
        drawHomePhotoBehindSystemBars = drawHomePhotoBehindSystemBars
    ) {
        AppNavigation(
            folderSelectionState = folderSelectionState,
            themePreference = themePreference,
            saveDestinationState = saveDestinationState,
            appVersion = appVersion,
            homeBackgroundUri = homeBackgroundUri,
            includedHomeBackground = includedHomeBackground,
            onSelectRecommendedFolder = { folderPicker.launch(recommendedStatusesParentUri()) },
            onSelectFolder = { folderPicker.launch(null) },
            onSelectHomeBackground = { homeBackgroundPicker.launch(arrayOf("image/*")) },
            onClearHomeBackground = settingsViewModel::clearHomeBackground,
            onSelectIncludedHomeBackground = settingsViewModel::selectIncludedHomeBackground,
            onSelectSaveDestination = { saveDestinationPicker.launch(null) },
            onUseDefaultSaveDestination = settingsViewModel::useDefaultSaveDestination,
            onThemePreferenceSelected = settingsViewModel::selectTheme,
            resetState = resetState,
            onResetSettings = settingsViewModel::resetSettings,
            onResetMessageDismissed = settingsViewModel::clearResetMessage,
            onOpenPrivacyPolicy = { context.openPrivacyPolicy() },
            onShareApp = { context.shareEstadoGo() },
            onRateApp = { context.rateEstadoGo() },
            onValidateHomeBackground = settingsViewModel::validateHomeBackground,
            onHomePhotoSystemBarsStateChanged = { drawHomePhotoBehindSystemBars = it }
        )
    }
}

private const val ExternalStorageDocumentsAuthority = "com.android.externalstorage.documents"
private const val RecommendedStatusesParentDocumentId =
    "primary:Android/media/com.whatsapp/WhatsApp/Media"

private fun recommendedStatusesParentUri(): Uri {
    return DocumentsContract.buildDocumentUri(
        ExternalStorageDocumentsAuthority,
        RecommendedStatusesParentDocumentId
    )
}

private fun Uri.isRecommendedStatusesParentFolder(): Boolean {
    return runCatching { DocumentsContract.getTreeDocumentId(this) }
        .getOrNull()
        .equals(RecommendedStatusesParentDocumentId, ignoreCase = true)
}

private fun Context.installedVersionName(): String {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    return packageInfo.versionName.orEmpty()
}

private fun Context.openPrivacyPolicy() {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.settings_privacy_policy_url)))
    runCatching { startActivity(intent) }
        .onFailure { Toast.makeText(this, R.string.app_action_error, Toast.LENGTH_SHORT).show() }
}

private fun Context.shareEstadoGo() {
    val playStoreUrl = googlePlayWebUrl()
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, getString(R.string.app_share_text, playStoreUrl))
    }
    val chooser = Intent.createChooser(sendIntent, getString(R.string.app_share_chooser_title))
    runCatching { startActivity(chooser) }
        .onFailure { Toast.makeText(this, R.string.app_share_error, Toast.LENGTH_SHORT).show() }
}

private fun Context.rateEstadoGo() {
    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
    try {
        startActivity(marketIntent)
    } catch (exception: ActivityNotFoundException) {
        openGooglePlayWeb()
    } catch (exception: Exception) {
        openGooglePlayWeb()
    }
}

private fun Context.openGooglePlayWeb() {
    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(googlePlayWebUrl()))
    runCatching { startActivity(webIntent) }
        .onFailure { Toast.makeText(this, R.string.app_rate_error, Toast.LENGTH_SHORT).show() }
}

private fun Context.googlePlayWebUrl(): String {
    return "https://play.google.com/store/apps/details?id=$packageName"
}
