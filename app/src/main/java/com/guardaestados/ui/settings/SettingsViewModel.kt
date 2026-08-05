package com.guardaestados.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.guardaestados.data.folder.FolderSelectionRepository
import com.guardaestados.data.settings.AppSettingsRepository
import com.guardaestados.data.settings.AppThemePreference
import com.guardaestados.data.settings.SaveDestinationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val folderSelectionRepository: FolderSelectionRepository
) : ViewModel() {
    val themePreference: StateFlow<AppThemePreference> = appSettingsRepository.themePreference.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppThemePreference.System
    )

    val saveDestinationState: StateFlow<SaveDestinationState> = appSettingsRepository.saveDestinationState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SaveDestinationState.Default
    )

    private val _resetState = MutableStateFlow<SettingsResetState>(SettingsResetState.Idle)
    val resetState: StateFlow<SettingsResetState> = _resetState.asStateFlow()

    fun selectTheme(themePreference: AppThemePreference) {
        viewModelScope.launch {
            appSettingsRepository.saveThemePreference(themePreference)
        }
    }

    fun selectSaveDestination(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            appSettingsRepository.saveDestinationFolder(uri)
        }
    }

    fun useDefaultSaveDestination() {
        viewModelScope.launch(Dispatchers.IO) {
            appSettingsRepository.resetSaveDestinationFolder()
        }
    }

    fun resetSettings() {
        if (_resetState.value == SettingsResetState.Resetting) return

        viewModelScope.launch(Dispatchers.IO) {
            _resetState.value = SettingsResetState.Resetting
            folderSelectionRepository.forgetSelectedFolder()
            appSettingsRepository.resetSaveDestinationFolder()
            appSettingsRepository.resetThemePreference()
            _resetState.value = SettingsResetState.Success
        }
    }

    fun clearResetMessage() {
        _resetState.value = SettingsResetState.Idle
    }
}

sealed interface SettingsResetState {
    data object Idle : SettingsResetState
    data object Resetting : SettingsResetState
    data object Success : SettingsResetState
}

class SettingsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val appContext = context.applicationContext
            return SettingsViewModel(
                appSettingsRepository = AppSettingsRepository(appContext),
                folderSelectionRepository = FolderSelectionRepository(appContext)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}