package com.guardaestados.ui

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guardaestados.R
import com.guardaestados.ads.ConsentManager
import com.guardaestados.data.folder.FolderSelectionRepository
import com.guardaestados.data.folder.FolderSelectionState
import com.guardaestados.data.folder.takeSaveDestinationFolderPermission
import com.guardaestados.data.folder.takeSelectedFolderPermission
import com.guardaestados.ui.navigation.AppNavigation
import com.guardaestados.ui.ads.AppOpenAdManager
import com.guardaestados.ui.settings.SettingsViewModel
import com.guardaestados.ui.settings.SettingsViewModelFactory
import com.guardaestados.ui.theme.GuardaEstadosTheme
import kotlinx.coroutines.launch

@Composable
fun GuardaEstadosApp(shouldAttemptAppOpenAd: Boolean = false) {
    val context = LocalContext.current
    val repository = remember(context) { FolderSelectionRepository(context) }
    val consentManager = remember(context.applicationContext) {
        ConsentManager(context.applicationContext)
    }
    val appOpenAdManager = remember(context.applicationContext, shouldAttemptAppOpenAd) {
        AppOpenAdManager(context.applicationContext).also { manager ->
            if (shouldAttemptAppOpenAd) {
                manager.markColdStartObserved()
            }
        }
    }
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = remember(context) { SettingsViewModelFactory(context) }
    )
    val folderSelectionState by repository.selectionState.collectAsState(
        initial = FolderSelectionState.Loading
    )
    val themePreference by settingsViewModel.themePreference.collectAsState()
    val resetState by settingsViewModel.resetState.collectAsState()
    val saveDestinationState by settingsViewModel.saveDestinationState.collectAsState()
    val adsPrivacyState by consentManager.privacyState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val folderPicker = rememberLauncherForActivityResult(
        contract = ReadOnlyOpenDocumentTree()
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        if (uri.isRecommendedStatusesParentFolder()) {
            Toast.makeText(context, R.string.folder_media_selection_rejected, Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        if (!context.takeSelectedFolderPermission(uri)) {
            Toast.makeText(context, R.string.folder_permission_persist_error, Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            repository.saveSelectedFolder(uri)
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
    val activity = context.findActivity()

    LaunchedEffect(activity) {
        activity?.let(consentManager::updateConsent)
    }

    GuardaEstadosTheme(themeMode = themePreference.toThemeMode(systemDarkTheme)) {
        AppNavigation(
            folderSelectionState = folderSelectionState,
            themePreference = themePreference,
            saveDestinationState = saveDestinationState,
            appVersion = appVersion,
            onSelectRecommendedFolder = { folderPicker.launch(recommendedStatusesParentUri()) },
            onSelectFolder = { folderPicker.launch(null) },
            onSelectSaveDestination = { saveDestinationPicker.launch(null) },
            onUseDefaultSaveDestination = settingsViewModel::useDefaultSaveDestination,
            onThemePreferenceSelected = settingsViewModel::selectTheme,
            resetState = resetState,
            onResetSettings = settingsViewModel::resetSettings,
            onResetMessageDismissed = settingsViewModel::clearResetMessage,
            onOpenPrivacyPolicy = { context.openPrivacyPolicy() },
            adsCanRequest = adsPrivacyState.canRequestAds,
            adsPrivacyOptionsAvailable = adsPrivacyState.privacyOptionsAvailable,
            onColdStartHomeReadyForAppOpenAd = {
                if (shouldAttemptAppOpenAd) {
                    activity?.let(appOpenAdManager::loadAndShowOnColdStart)
                }
            },
            onOpenAdsPrivacyOptions = {
                val currentActivity = context.findActivity()
                if (currentActivity == null) {
                    Toast.makeText(context, R.string.privacy_info_ads_options_error, Toast.LENGTH_SHORT).show()
                } else {
                    consentManager.showPrivacyOptions(
                        activity = currentActivity,
                        onUnavailable = {
                            Toast.makeText(
                                context,
                                R.string.privacy_info_ads_options_unavailable,
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onError = {
                            Toast.makeText(
                                context,
                                R.string.privacy_info_ads_options_error,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            },
            onShareApp = { context.shareEstadoGo() },
            onRateApp = { context.rateEstadoGo() }
        )
    }
}

private const val ExternalStorageDocumentsAuthority = "com.android.externalstorage.documents"
private const val RecommendedStatusesParentDocumentId =
    "primary:Android/media/com.whatsapp/WhatsApp/Media"

private class ReadOnlyOpenDocumentTree : ActivityResultContract<Uri?, Uri?>() {
    override fun createIntent(context: Context, input: Uri?): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            input?.let { initialUri ->
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent?.data.takeIf { resultCode == RESULT_OK }
    }
}

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

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
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
