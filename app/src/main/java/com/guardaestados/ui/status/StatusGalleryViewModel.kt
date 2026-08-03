package com.guardaestados.ui.status

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.guardaestados.data.folder.FolderSelectionRepository
import com.guardaestados.data.folder.FolderSelectionState
import com.guardaestados.data.status.StatusImageRepository
import com.guardaestados.domain.status.LoadStatusImagesUseCase
import com.guardaestados.domain.status.StatusGalleryState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatusGalleryViewModel(
    private val folderSelectionRepository: FolderSelectionRepository,
    private val loadStatusImages: LoadStatusImagesUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<StatusGalleryState>(StatusGalleryState.Loading)
    val uiState: StateFlow<StatusGalleryState> = _uiState.asStateFlow()

    private var latestFolderState: FolderSelectionState = FolderSelectionState.Loading

    init {
        viewModelScope.launch {
            folderSelectionRepository.selectionState.collect { folderState ->
                latestFolderState = folderState
                reload(folderState)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            reload(latestFolderState)
        }
    }

    private suspend fun reload(folderState: FolderSelectionState) {
        _uiState.value = StatusGalleryState.Loading
        _uiState.value = withContext(Dispatchers.IO) {
            loadStatusImages.execute(folderState)
        }
    }
}

class StatusGalleryViewModelFactory(
    context: Context
) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatusGalleryViewModel::class.java)) {
            val folderSelectionRepository = FolderSelectionRepository(appContext)
            val statusImageRepository = StatusImageRepository(appContext)
            val loadStatusImages = LoadStatusImagesUseCase(statusImageRepository)
            return StatusGalleryViewModel(folderSelectionRepository, loadStatusImages) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
