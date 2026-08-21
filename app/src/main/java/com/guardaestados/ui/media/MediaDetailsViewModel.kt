package com.guardaestados.ui.media

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.guardaestados.data.media.AndroidMediaDetailsRepository
import com.guardaestados.domain.media.MediaDetails
import com.guardaestados.domain.saved.SavedImage
import com.guardaestados.domain.status.StatusImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaDetailsViewModel(
    private val repository: AndroidMediaDetailsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<MediaDetailsUiState>(MediaDetailsUiState.Idle)
    val uiState: StateFlow<MediaDetailsUiState> = _uiState.asStateFlow()

    fun loadStatusDetails(image: StatusImage) {
        _uiState.value = MediaDetailsUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = MediaDetailsUiState.Content(repository.loadStatusDetails(image))
        }
    }

    fun loadSavedDetails(image: SavedImage) {
        _uiState.value = MediaDetailsUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = MediaDetailsUiState.Content(repository.loadSavedDetails(image))
        }
    }

    fun clear() {
        _uiState.value = MediaDetailsUiState.Idle
    }
}

sealed interface MediaDetailsUiState {
    data object Idle : MediaDetailsUiState
    data object Loading : MediaDetailsUiState
    data class Content(val details: MediaDetails) : MediaDetailsUiState
}

class MediaDetailsViewModelFactory(
    context: Context
) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaDetailsViewModel::class.java)) {
            return MediaDetailsViewModel(AndroidMediaDetailsRepository(appContext)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
